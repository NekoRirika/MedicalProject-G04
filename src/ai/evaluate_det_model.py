import os
import torch
import multiprocessing
import warnings
warnings.filterwarnings('ignore')

TEST_CONFIG = {
    "model_type": "yolo",
    "model_path": "runs/detect/runs/detect/rsna_yolov9/weights/best.pt",
    "yaml_path": "rsna_yolo.yaml",
    "img_size": 640,
    "conf": 0.25,
    "iou": 0.45,
    "batch_size": 8,
    "device": 0 if torch.cuda.is_available() else "cpu"
}

def main():
    if __name__ == "__main__":
        multiprocessing.set_start_method("spawn", force=True)
    
    config = TEST_CONFIG
    print("="*80)
    print("检测模型测试启动")
    print(f"模型类型：{config['model_type']}")
    print(f"数据集配置：{config['yaml_path']}")
    print("="*80)

    print("\n正在加载检测模型...")
    if config["model_type"] == "yolo":
        from ultralytics import YOLO
        model = YOLO(config["model_path"]).to(config["device"])
    elif config["model_type"] == "rtdetr":
        from ultralytics import RTDETR
        model = RTDETR(config["model_path"]).to(config["device"])
    else:
        raise ValueError(f"不支持的模型类型：{config['model_type']}")
    print("检测模型加载完成")

    print("\n开始验证集测试...")
    results = model.val(
        data=config["yaml_path"],
        batch=config["batch_size"],
        imgsz=config["img_size"],
        device=config["device"],
        conf=config["conf"],
        iou=config["iou"],
        workers=4,
        save_json=False,
        save_conf=False,
        plots=False, 
        verbose=True,
        single_cls=True 
    )

    print("\n" + "="*80)
    print("检测模型验证集测试结果")
    print("="*80)

    map50 = results.box.map50
    map = results.box.map
    precision = results.box.mp
    recall = results.box.mr
    mean_iou = results.box.mean_iou if hasattr(results.box, "mean_iou") else 0.0

    print(f"mAP50 (IoU=0.5):  {map50:.4f}")
    print(f"mAP50-95:          {map:.4f}")
    print(f"Precision:         {precision:.4f}")
    print(f"Recall:            {recall:.4f}")
    print(f"Mean IoU:          {mean_iou:.4f}")

    print("\n单类别详细指标：")
    print(f"病灶类别 | Precision: {results.box.ap_class()[0]:.4f} | Recall: {results.box.ar_class()[0]:.4f} | AP50: {results.box.ap50_class()[0]:.4f}")

    print("\n检测模型测试完成！")
    print("="*80)

if __name__ == "__main__":
    main()