from ultralytics import RTDETR
import torch
import multiprocessing
import os
import shutil

YAML_PATH = "rsna_yolo.yaml"
EPOCHS = 150
BATCH_SIZE = 8
IMGSZ = 800
DEVICE = 0 if torch.cuda.is_available() else "cpu"
PATIENCE = 35

AUTO_SAVE_DIR = "./runs/detect/rsna_rtdetr"
MANUAL_SAVE_DIR = "./manual_saved_models_rtdetr"
os.makedirs(MANUAL_SAVE_DIR, exist_ok=True)

def main():
    print("RT-DETR 训练启动")
    print(f"📌 数据集配置：{YAML_PATH}")
    print(f"📌 训练轮次：{EPOCHS}")

    model = RTDETR("rtdetr-l.pt")

    model.train(
        data=YAML_PATH,
        epochs=EPOCHS,
        batch=BATCH_SIZE,
        imgsz=IMGSZ,
        device=DEVICE,
        patience=PATIENCE,
        workers=0,
        cache=False,
        amp=False,
        single_cls=True,
        deterministic=False,
        project=os.path.dirname(AUTO_SAVE_DIR),
        name=os.path.basename(AUTO_SAVE_DIR),
        exist_ok=True,

        lr0=3e-6,
        lrf=0.005,
        warmup_epochs=30,
        optimizer="AdamW",
        cos_lr=True,
        box=2.0,
        cls=3.0,
        dfl=1.0,

        hsv_h=0.0,
        hsv_s=0.0,
        hsv_v=0.0,
        degrees=0.0,
        translate=0.0,
        scale=0.0,
        fliplr=0.0,
        mosaic=0.0,
        mixup=0.0,
        copy_paste=0.0,
    )

    print("\n" + "="*60)
    print("正在手动备份最优模型...")
    
    auto_best_path = os.path.join(AUTO_SAVE_DIR, "weights", "best.pt")
    manual_best_path = os.path.join(MANUAL_SAVE_DIR, "rtdetr-l_rsna_clean_best.pt")
    
    if os.path.exists(auto_best_path):
        shutil.copy(auto_best_path, manual_best_path)
        model_size = os.path.getsize(manual_best_path) / 1024 / 1024
        print(f"最优模型已备份至：{manual_best_path}")
        print(f"模型大小：{model_size:.1f}MB")
    else:
        print(f"未找到自动保存的最优模型：{auto_best_path}")
        # 尝试保存当前模型
        current_model_path = os.path.join(AUTO_SAVE_DIR, "weights", "last.pt")
        if os.path.exists(current_model_path):
            shutil.copy(current_model_path, manual_best_path)
            print(f"已备份最后一轮模型至：{manual_best_path}")
    
    print("="*60)

    print("\nRT-DETR 训练完成！")

if __name__ == "__main__":
    main()