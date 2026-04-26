import os
import cv2
import pydicom
import pandas as pd
import numpy as np
import torch
import torch.nn as nn
import torchvision
from torch.utils.data import Dataset, DataLoader
from sklearn.model_selection import train_test_split
from tqdm import tqdm
import albumentations as A
from stable_baselines3 import PPO
from stable_baselines3.common.torch_layers import BaseFeaturesExtractor
import gymnasium as gym
from gymnasium import spaces

CONFIG = {
    "dataset_root": "./dataset",
    "img_size": 224,
    "batch_size": 32,
    "num_workers": 0,
    "random_seed": 42,
    "sample_num": 1000
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
        return np.random.randint(0, 255, (CONFIG["img_size"], CONFIG["img_size"]), dtype=np.uint8), (1024, 1024)

def calculate_mean_std(train_df, img_dir):
    print(f"\n采样{CONFIG['sample_num']}张训练集图像计算Mean/Std...")
    pixel_sum = 0.0
    pixel_sum_sq = 0.0
    pixel_count = 0.0
    sample_df = train_df.sample(n=min(CONFIG['sample_num'], len(train_df)), random_state=CONFIG["random_seed"])
    
    for idx, row in tqdm(sample_df.iterrows(), total=len(sample_df)):
        pid = row['patientId']
        dicom_path = os.path.join(img_dir, f"{pid}.dcm")
        img, _ = dicom_to_gray(dicom_path)
        img = cv2.resize(img, (CONFIG["img_size"], CONFIG["img_size"]))
        img_float = img.astype(np.float32)
        pixel_sum += img_float.sum()
        pixel_sum_sq += (img_float ** 2).sum()
        pixel_count += img_float.size
    
    if pixel_count == 0:
        mean, std = 0.0, 1.0
    else:
        mean = pixel_sum / pixel_count
        std = np.sqrt((pixel_sum_sq / pixel_count) - (mean ** 2))
        mean /= 255.0
        std /= 255.0
    
    if std < 1e-6:
        std = 1e-6
    print(f"Mean/Std计算完成 | Mean: {mean:.4f}, Std: {std:.4f}")
    return mean, std

class RSNADetectionEnv(gym.Env):
    def __init__(self, df, img_dir, mean, std, img_size=224, max_steps=15):
        super().__init__()
        self.df = df[df['Target'] == 1].reset_index(drop=True)
        self.patient_group = self.df.groupby('patientId')
        self.valid_pids = list(self.patient_group.groups.keys())
        if len(self.valid_pids) == 0:
            raise ValueError("数据集中没有找到有病灶的样本，请检查标注文件")
        
        self.img_dir = img_dir
        self.mean = mean
        self.std = std
        self.img_size = img_size
        self.max_steps = max_steps 
        
        self.action_space = spaces.Discrete(7)
        
        self.observation_space = spaces.Dict({
            'image': spaces.Box(low=-np.inf, high=np.inf, shape=(1, img_size, img_size), dtype=np.float32),
            'box': spaces.Box(low=0.0, high=1.0, shape=(4,), dtype=np.float32),
            'offset': spaces.Box(low=-1.0, high=1.0, shape=(2,), dtype=np.float32)
        })

    def _load_and_process_img(self, pid):
        dicom_path = os.path.join(self.img_dir, f"{pid}.dcm")
        img, (orig_h, orig_w) = dicom_to_gray(dicom_path)
        img = cv2.resize(img, (self.img_size, self.img_size))
        img = img.astype(np.float32) / 255.0
        img = (img - self.mean) / self.std
        img = np.expand_dims(img, axis=0)
        return img, (orig_h, orig_w)

    def _get_gt_boxes(self, pid, orig_shape):
        rows = self.patient_group.get_group(pid)
        orig_h, orig_w = orig_shape
        scale_x = self.img_size / orig_w
        scale_y = self.img_size / orig_h
        gt_boxes = []
        for _, row in rows.iterrows():
            x, y, w, h = row['x'], row['y'], row['width'], row['height']
            x1, y1 = x * scale_x, y * scale_y
            x2, y2 = (x + w) * scale_x, (y + h) * scale_y
            box = np.array([x1, y1, x2, y2], dtype=np.float32) / self.img_size
            gt_boxes.append(np.clip(box, 0.0, 1.0))
        return np.array(gt_boxes)

    def _init_box(self):
        cx = np.random.uniform(0.2, 0.8)
        cy = np.random.uniform(0.2, 0.8)
        w = np.random.uniform(0.2, 0.6)
        h = np.random.uniform(0.2, 0.6)
        x1 = np.clip(cx - w/2, 0.0, 0.99)
        y1 = np.clip(cy - h/2, 0.0, 0.99)
        x2 = np.clip(cx + w/2, 0.01, 1.0)
        y2 = np.clip(cy + h/2, 0.01, 1.0)
        return np.array([x1, y1, x2, y2], dtype=np.float32)

    def _calculate_max_iou(self, box, gt_boxes):
        max_iou = 0.0
        best_gt = gt_boxes[0]
        for gt in gt_boxes:
            x1_1, y1_1, x2_1, y2_1 = box
            x1_2, y1_2, x2_2, y2_2 = gt
            
            inter_x1 = max(x1_1, x1_2)
            inter_y1 = max(y1_1, y1_2)
            inter_x2 = min(x2_1, x2_2)
            inter_y2 = min(y2_1, y2_2)
            inter_area = max(0.0, inter_x2 - inter_x1) * max(0.0, inter_y2 - inter_y1)
            
            area1 = (x2_1 - x1_1) * (y2_1 - y1_1)
            area2 = (x2_2 - x1_2) * (y2_2 - y1_2)
            union_area = area1 + area2 - inter_area
            iou = inter_area / union_area if union_area > 0 else 0.0
            
            if iou > max_iou:
                max_iou = iou
                best_gt = gt
        return max_iou, best_gt

    def _apply_action(self, action):
        box = self.current_box.copy()
        x1, y1, x2, y2 = box
        w, h = x2 - x1, y2 - y1
        
        base_step = 0.03
        step = base_step * max(w, h) * 2
        scale = 0.15
        
        if action == 0: x1 -= step; x2 -= step
        elif action == 1: x1 += step; x2 += step
        elif action == 2: y1 -= step; y2 -= step
        elif action == 3: y1 += step; y2 += step
        elif action == 4:
            x1 -= w * scale / 2; y1 -= h * scale / 2
            x2 += w * scale / 2; y2 += h * scale / 2
        elif action == 5:
            x1 += w * scale / 2; y1 += h * scale / 2
            x2 -= w * scale / 2; y2 -= h * scale / 2
        
        box = np.clip([x1, y1, x2, y2], 0.0, 1.0)
        if box[2] <= box[0]: box[2] = box[0] + 0.01
        if box[3] <= box[1]: box[3] = box[1] + 0.01
        return box.astype(np.float32)

    def reset(self, seed=None, options=None):
        super().reset(seed=seed)
        self.current_pid = self.np_random.choice(self.valid_pids)
        self.current_img, orig_shape = self._load_and_process_img(self.current_pid)
        self.current_gt_boxes = self._get_gt_boxes(self.current_pid, orig_shape)
        self.current_box = self._init_box()
        self.current_step = 0
        self.last_action = -1
        self.repeat_count = 0
        
        current_iou, best_gt = self._calculate_max_iou(self.current_box, self.current_gt_boxes)
        cx_cur, cy_cur = (self.current_box[0]+self.current_box[2])/2, (self.current_box[1]+self.current_box[3])/2
        cx_gt, cy_gt = (best_gt[0]+best_gt[2])/2, (best_gt[1]+best_gt[3])/2
        offset = np.array([cx_cur - cx_gt, cy_cur - cy_gt], dtype=np.float32)
        
        return {
            'image': self.current_img,
            'box': self.current_box,
            'offset': offset
        }, {}

    def step(self, action):
        self.current_step += 1
        if action == self.last_action:
            self.repeat_count += 1
        else:
            self.repeat_count = 0
        self.last_action = action
        
        prev_iou, best_gt = self._calculate_max_iou(self.current_box, self.current_gt_boxes)
        cx_cur_prev, cy_cur_prev = (self.current_box[0]+self.current_box[2])/2, (self.current_box[1]+self.current_box[3])/2
        cx_gt, cy_gt = (best_gt[0]+best_gt[2])/2, (best_gt[1]+best_gt[3])/2
        prev_dist = np.sqrt((cx_cur_prev - cx_gt)**2 + (cy_cur_prev - cy_gt)**2)
        
        if action != 6:
            self.current_box = self._apply_action(action)
        
        current_iou, best_gt = self._calculate_max_iou(self.current_box, self.current_gt_boxes)
        cx_cur, cy_cur = (self.current_box[0]+self.current_box[2])/2, (self.current_box[1]+self.current_box[3])/2
        current_dist = np.sqrt((cx_cur - cx_gt)**2 + (cy_cur - cy_gt)**2)
        offset = np.array([cx_cur - cx_gt, cy_cur - cy_gt], dtype=np.float32)
        
        reward = 0.0
        reward += current_iou * 3.0 
        iou_delta = current_iou - prev_iou
        reward += iou_delta * 20.0 if iou_delta > 0 else iou_delta * 10.0
        dist_delta = prev_dist - current_dist
        if self.repeat_count >= 2: reward -= 0.5 * self.repeat_count  
        w, h = self.current_box[2]-self.current_box[0], self.current_box[3]-self.current_box[1]
        if w < 0.05 or h < 0.05 or w > 0.95 or h > 0.95: reward -= 2.0 
        
        done = False
        if action == 6:
            done = True
            if current_iou >= 0.7: reward += 50.0
            elif current_iou >= 0.5: reward += 20.0
            elif current_iou >= 0.3: reward += 5.0
            else: reward -= 15.0
        elif self.current_step >= self.max_steps:
            done = True
            reward -= 10.0
        
        return (
            {'image': self.current_img, 'box': self.current_box, 'offset': offset},
            reward,
            done,
            False,
            {'iou': current_iou, 'step': self.current_step, 'dist': current_dist}
        )

class CustomCNNExtractor(BaseFeaturesExtractor):
    def __init__(self, observation_space: spaces.Dict, features_dim=256):
        super().__init__(observation_space, features_dim)
        
        self.cnn = torchvision.models.resnet18(weights=torchvision.models.ResNet18_Weights.IMAGENET1K_V1)
        self.cnn.conv1 = nn.Conv2d(1, 64, kernel_size=7, stride=2, padding=3, bias=False)
        self.cnn.fc = nn.Sequential(
            nn.Linear(512, 256),
            nn.ReLU(),
            nn.Dropout(0.2)
        )
        
        self.box_mlp = nn.Sequential(
            nn.Linear(4 + 2, 64),
            nn.ReLU(),
            nn.Linear(64, 32)
        )
        
        self.fusion = nn.Sequential(
            nn.Linear(256 + 32, features_dim),
            nn.ReLU(),
            nn.LayerNorm(features_dim)
        )

    def forward(self, observations):
        img_feat = self.cnn(observations['image'])
        box_input = torch.cat([observations['box'], observations['offset']], dim=1)
        box_feat = self.box_mlp(box_input)
        combined = torch.cat([img_feat, box_feat], dim=1)
        return self.fusion(combined)

if __name__ == "__main__":
    label_path = os.path.join(CONFIG["dataset_root"], "stage_2_train_labels.csv")
    if not os.path.exists(label_path):
        raise FileNotFoundError(f"标注文件不存在：{label_path}")
    label_df = pd.read_csv(label_path)
    img_dir = os.path.join(CONFIG["dataset_root"], "stage_2_train_images")
    
    mean_std_path = os.path.join(CONFIG["dataset_root"], "mean_std.npy")
    if os.path.exists(mean_std_path):
        mean, std = np.load(mean_std_path)
        print(f"加载已保存的 Mean/Std | Mean: {mean:.4f}, Std: {std:.4f}")
    else:
        df_classification = label_df[['patientId', 'Target']].drop_duplicates()
        mean, std = calculate_mean_std(df_classification, img_dir)
        np.save(mean_std_path, [mean, std])

    env = RSNADetectionEnv(
        df=label_df,
        img_dir=img_dir,
        mean=mean,
        std=std,
        img_size=CONFIG["img_size"],
        max_steps=15
    )

    policy_kwargs = {
        "features_extractor_class": CustomCNNExtractor,
        "features_extractor_kwargs": {"features_dim": 256},
        "net_arch": dict(pi=[256, 128, 64], vf=[256, 128, 64]),
        "normalize_images": False
    }

    try:
        import tensorboard
        tensorboard_log = "./rsna_model_logs/"
    except ImportError:
        print("警告：tensorboard未安装，已关闭日志记录")
        tensorboard_log = None

    model = PPO(
        policy="MultiInputPolicy",
        env=env,
        policy_kwargs=policy_kwargs,
        verbose=1,
        learning_rate=2e-4,
        n_steps=1024,
        batch_size=128,
        gamma=0.95,
        gae_lambda=0.95,
        clip_range=0.2,
        ent_coef=0.01,
        tensorboard_log=tensorboard_log
    )

    print("开始训练...")
    model.learn(total_timesteps=500000)

    model.save("rsna_detection_model")
    print("优化后的模型已保存为 rsna_detection_model")

    # 7. 测试推理
    obs, _ = env.reset()
    print("\n开始测试模型推理：")
    for step in range(15):
        action, _states = model.predict(obs, deterministic=True)
        obs, rewards, done, _, info = env.step(action)
        print(f"Step {step+1} | 动作: {action} | IoU: {info['iou']:.4f} | 奖励: {rewards:.4f}")
        if done:
            print(f"Episode 结束 | 最终IoU: {info['iou']:.4f}")
            break