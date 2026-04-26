import os
import cv2
import pydicom
import numpy as np
import torch
import torch.nn as nn
import torchvision
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from stable_baselines3 import PPO
from stable_baselines3.common.torch_layers import BaseFeaturesExtractor

TEST_IMAGE_PATH = "./dataset/stage_2_train_images/00704310-78a8-4b38-8475-49f4573b2dbb.dcm"
MODEL_PATH = "new_model_d"
GT_BOX = [696.0, 572.0, 244.0, 327.0]  
CONFIG = {
    "img_size": 224,
    "random_seed": 42
}

ACTION_DICT = {
    0: "左移", 1: "右移", 2: "上移", 3: "下移",
    4: "扩大", 5: "缩小", 6: "终止"
}

def dicom_to_gray(dicom_path):
    try:
        dicom = pydicom.dcmread(dicom_path, force=True, defer_size="16MB")
        try:
            img = dicom.pixel_array.astype(np.float32)
        except Exception as e:
            from pydicom.pixel_data_handlers.util import apply_modality_lut
            img = apply_modality_lut(dicom.pixel_array, dicom).astype(np.float32)
        
        if img.max() == img.min():
            img = np.zeros_like(img)
        else:
            img = (img - img.min()) / (img.max() - img.min()) * 255
        return img.astype(np.uint8), (dicom.Rows, dicom.Columns)
    except Exception as e:
        print(f"DICOM加载失败: {dicom_path}")
        raise e

class CustomCNNExtractor(BaseFeaturesExtractor):
    def __init__(self, observation_space, features_dim=256):
        super().__init__(observation_space, features_dim)
        self.cnn = torchvision.models.resnet18(weights=torchvision.models.ResNet18_Weights.IMAGENET1K_V1)
        self.cnn.conv1 = nn.Conv2d(1, 64, kernel_size=7, stride=2, padding=3, bias=False)
        self.cnn.fc = nn.Sequential(nn.Linear(512, 256), nn.ReLU(), nn.Dropout(0.2))
        self.box_mlp = nn.Sequential(nn.Linear(4 + 2, 64), nn.ReLU(), nn.Linear(64, 32))
        self.fusion = nn.Sequential(nn.Linear(256 + 32, features_dim), nn.ReLU(), nn.LayerNorm(features_dim))

    def forward(self, observations):
        img_feat = self.cnn(observations['image'])
        box_input = torch.cat([observations['box'], observations['offset']], dim=1)
        box_feat = self.box_mlp(box_input)
        return self.fusion(torch.cat([img_feat, box_feat], dim=1))

def preprocess_image(image_path, mean, std):
    if image_path.lower().endswith(".dcm"):
        img_gray, orig_shape = dicom_to_gray(image_path)
    else:
        img_gray = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
        orig_shape = img_gray.shape[:2]
    
    original_vis_img = cv2.resize(img_gray, (CONFIG["img_size"], CONFIG["img_size"]))
    
    img = cv2.resize(img_gray, (CONFIG["img_size"], CONFIG["img_size"]))
    img = img.astype(np.float32) / 255.0
    img = (img - mean) / std
    img_np = np.expand_dims(img, axis=0).astype(np.float32)
    
    return img_np, original_vis_img, orig_shape

def convert_gt_box(gt_box, orig_shape, img_size):
    orig_h, orig_w = orig_shape
    if len(gt_box) == 4:
        # RSNA原生格式 [x,y,w,h]
        if gt_box[2] > 0 and gt_box[3] > 0 and gt_box[0]+gt_box[2] <= orig_w and gt_box[1]+gt_box[3] <= orig_h:
            x1, y1 = gt_box[0], gt_box[1]
            x2, y2 = gt_box[0] + gt_box[2], gt_box[1] + gt_box[3]
            # 归一化
            scale_x = img_size / orig_w
            scale_y = img_size / orig_h
            box = np.array([x1*scale_x, y1*scale_y, x2*scale_x, y2*scale_y], dtype=np.float32) / img_size
            return np.clip(box, 0.0, 1.0)
        # 已经是归一化[x1,y1,x2,y2]
        else:
            return np.clip(np.array(gt_box, dtype=np.float32), 0.0, 1.0)
    else:
        raise ValueError("标注格式错误，必须是 [x,y,width,height] 或 [x1,y1,x2,y2]")

def box_to_original_size(normalized_box, orig_shape):
    orig_h, orig_w = orig_shape
    x1, y1, x2, y2 = normalized_box
    orig_x1 = int(x1 * orig_w)
    orig_y1 = int(y1 * orig_h)
    orig_x2 = int(x2 * orig_w)
    orig_y2 = int(y2 * orig_h)
    return [orig_x1, orig_y1, orig_x2, orig_y2]

def visualize_single_result(original_img, pred_box, gt_box=None, save_path="single_test_result.png"):
    """可视化单张测试结果"""
    fig, ax = plt.subplots(1, figsize=(8, 8))
    ax.imshow(original_img, cmap='gray')
    img_size = original_img.shape[0]

    if gt_box is not None:
        x1, y1, x2, y2 = gt_box * img_size
        rect = patches.Rectangle(
            (x1, y1), x2-x1, y2-y1,
            linewidth=3, edgecolor='lime', facecolor='none', label='真实病灶'
        )
        ax.add_patch(rect)

    x1, y1, x2, y2 = pred_box * img_size
    rect = patches.Rectangle(
        (x1, y1), x2-x1, y2-y1,
        linewidth=3, edgecolor='red', facecolor='none', label='Agent预测病灶'
    )
    ax.add_patch(rect)

    plt.legend(loc='upper right', fontsize=12)
    plt.axis('off')
    plt.tight_layout()
    plt.savefig(save_path, dpi=150, bbox_inches='tight')
    print(f"\n可视化结果已保存至: {os.path.abspath(save_path)}")
    plt.show()

if __name__ == "__main__":
    mean_std_path = os.path.join("./dataset", "mean_std.npy")
    if not os.path.exists(mean_std_path):
        raise FileNotFoundError(f"找不到mean_std.npy，请确保训练时生成的该文件在./dataset/目录下")
    mean, std = np.load(mean_std_path)
    print(f"加载训练配置 | Mean: {mean:.4f}, Std: {std:.4f}")

    print(f"正在加载模型: {MODEL_PATH}")
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    custom_objects = {"CustomCNNExtractor": CustomCNNExtractor}
    model = PPO.load(MODEL_PATH, custom_objects=custom_objects, device=device)
    print(f"模型加载成功！运行设备: {device}")

    print(f"正在加载测试图像: {TEST_IMAGE_PATH}")
    img_np, original_vis_img, orig_shape = preprocess_image(TEST_IMAGE_PATH, mean, std)
    print(f"图像加载完成 | 原始尺寸: {orig_shape[0]}x{orig_shape[1]}")

    gt_box_normalized = None
    if GT_BOX is not None:
        try:
            gt_box_normalized = convert_gt_box(GT_BOX, orig_shape, CONFIG["img_size"])
            print("真实标注加载完成")
        except Exception as e:
            print(f"标注解析失败: {e}，已忽略标注")
            gt_box_normalized = None

    print("\n" + "="*50)
    print("决策过程")
    print("="*50)

    current_box = np.array([0.25, 0.25, 0.75, 0.75], dtype=np.float32)
    max_steps = 15
    final_iou = 0.0

    for step in range(max_steps):
        cx_cur, cy_cur = (current_box[0]+current_box[2])/2, (current_box[1]+current_box[3])/2
        if gt_box_normalized is not None:
            cx_gt, cy_gt = (gt_box_normalized[0]+gt_box_normalized[2])/2, (gt_box_normalized[1]+gt_box_normalized[3])/2
        else:
            cx_gt, cy_gt = 0.5, 0.5 
        offset = np.array([cx_cur - cx_gt, cy_cur - cy_gt], dtype=np.float32)

        obs = {
            "image": img_np,  
            "box": current_box,
            "offset": offset  
        }

        action, _states = model.predict(obs, deterministic=True)
        action = int(action)

        if action != 6:
            x1, y1, x2, y2 = current_box
            w, h = x2 - x1, y2 - y1
            base_step = 0.03
            step_size = base_step * max(w, h) * 2
            scale = 0.15

            if action == 0: x1 -= step_size; x2 -= step_size
            elif action == 1: x1 += step_size; x2 += step_size
            elif action == 2: y1 -= step_size; y2 -= step_size
            elif action == 3: y1 += step_size; y2 += step_size
            elif action == 4:
                x1 -= w * scale / 2; y1 -= h * scale / 2
                x2 += w * scale / 2; y2 += h * scale / 2
            elif action == 5:
                x1 += w * scale / 2; y1 += h * scale / 2
                x2 -= w * scale / 2; y2 -= h * scale / 2

            current_box = np.clip([x1, y1, x2, y2], 0.0, 1.0)
            if current_box[2] <= current_box[0]: current_box[2] = current_box[0] + 0.01
            if current_box[3] <= current_box[1]: current_box[3] = current_box[1] + 0.01
            current_box = current_box.astype(np.float32)

        iou = 0.0
        if gt_box_normalized is not None:
            x1_1, y1_1, x2_1, y2_1 = current_box
            x1_2, y1_2, x2_2, y2_2 = gt_box_normalized
            inter_x1 = max(x1_1, x1_2)
            inter_y1 = max(y1_1, y1_2)
            inter_x2 = min(x2_1, x2_2)
            inter_y2 = min(y2_1, y2_2)
            inter_area = max(0.0, inter_x2 - inter_x1) * max(0.0, inter_y2 - inter_y1)
            area1 = (x2_1 - x1_1) * (y2_1 - y1_1)
            area2 = (x2_2 - x1_2) * (y2_2 - y1_2)
            union_area = area1 + area2 - inter_area
            iou = inter_area / union_area if union_area > 0 else 0.0
            final_iou = iou

        print(f"Step {step+1:2d} | 动作: {ACTION_DICT[action]:<4} | 当前IoU: {iou:.4f}")

        if action == 6:
            print(f"执行终止动作，结束定位")
            break

    print("\n" + "="*50)
    print("最终测试结果")
    print("="*50)
    orig_pred_box = box_to_original_size(current_box, orig_shape)
    print(f"预测病灶原始坐标 (x1,y1,x2,y2): {orig_pred_box}")
    print(f"预测病灶归一化坐标 (x1,y1,x2,y2): {np.round(current_box, 4)}")
    if gt_box_normalized is not None:
        print(f"最终IoU: {final_iou:.4f}")
    print("="*50)

    """
    visualize_single_result(
        original_img=original_vis_img,
        pred_box=current_box,
        gt_box=gt_box_normalized,
        save_path="single_test_result.png"
    )
    """