import os
import torch
import numpy as np
import pandas as pd
from tqdm import tqdm
from dataset import build_classification_dataloaders, CONFIG as DATA_CONFIG
from sklearn.metrics import (
    accuracy_score, recall_score, f1_score, roc_auc_score,
    precision_score, classification_report, confusion_matrix
)
import warnings
warnings.filterwarnings('ignore')

TEST_CONFIG = {
    "model_type": "efficientnetv2",
    "model_path": "trained_models_classification_efficientnetv2/best_classification_model.pth",
    "img_size": 512,
    "threshold": 0.5,
    "device": "cuda" if torch.cuda.is_available() else "cpu"
}

def load_classification_model(config):
    import timm
    device = config["device"]
    model_type = config["model_type"]
    model_path = config["model_path"]

    print(f"\n正在加载分类模型：{model_type}")
    ckpt = torch.load(model_path, map_location=device)
    if "model_state_dict" in ckpt:
        state_dict = ckpt["model_state_dict"]
    else:
        state_dict = ckpt

    if model_type == "resnet50":
        try:
            from resnet import resnet50_rsna
            model = resnet50_rsna(img_size=config["img_size"], num_classes=2)
        except:
            model = timm.create_model("resnet50", in_chans=1, num_classes=2, pretrained=False)
    elif model_type == "efficientnetv2":
        model = timm.create_model(
            "tf_efficientnetv2_m.in21k_ft_in1k",
            in_chans=1, num_classes=2, pretrained=False,
            drop_rate=0.5, drop_path_rate=0.2
        )
    else:
        raise ValueError(f"不支持的模型类型：{model_type}")

    model.load_state_dict(state_dict, strict=False)
    model = model.eval().to(device)
    print("分类模型加载完成")
    return model

# ===================== 3. 主测试流程 =====================
def main():
    config = TEST_CONFIG
    device = config["device"]
    print("="*80)
    print("分类模型验证集测试启动")
    print(f"模型类型：{config['model_type']}")
    print(f"模型路径：{config['model_path']}")
    print("="*80)

    try:
        _, test_loader, mean, std = build_classification_dataloaders()
        print(f"验证集构建完成 | Mean: {mean:.4f}, Std: {std:.4f}")
    except Exception as e:
        print(f"构建验证集失败：{str(e)}")
        return

    model = load_classification_model(config)

    print("\n开始验证集推理...")
    preds = []
    truths = []
    probs = []

    model.eval()
    with torch.no_grad():
        pbar = tqdm(test_loader, desc="验证进度")
        for batch in pbar:
            images = batch["image"].to(device)
            labels = batch["label"].to(device)
            
            # 推理
            logits = model(images)
            batch_probs = torch.softmax(logits, dim=1)[:, 1].cpu().numpy()
            batch_preds = (batch_probs >= config["threshold"]).astype(int)
            batch_truths = labels.cpu().numpy()
            
            # 收集结果
            truths.extend(batch_truths)
            preds.extend(batch_preds)
            probs.extend(batch_probs)

    # 4. 计算并输出指标
    print("\n" + "="*80)
    print("分类模型验证集测试结果")
    print(f"总样本数：{len(truths)}")
    print("="*80)

    if len(truths) == 0:
        print("无有效测试结果")
        return

    # 核心指标
    acc = accuracy_score(truths, preds)
    recall = recall_score(truths, preds, zero_division=0)
    precision = precision_score(truths, preds, zero_division=0)
    f1 = f1_score(truths, preds, zero_division=0)
    try:
        auc = roc_auc_score(truths, probs)
    except:
        auc = 0.0

    print(f"Accuracy:  {acc:.4f}")
    print(f"Recall:    {recall:.4f}")
    print(f"Precision: {precision:.4f}")
    print(f"F1 Score:  {f1:.4f}")
    print(f"AUC:       {auc:.4f}")

    print("\n混淆矩阵：")
    print(confusion_matrix(truths, preds))
    print("\n详细分类报告：")
    print(classification_report(truths, preds, target_names=["无病灶", "有病灶"], zero_division=0))

    print("\n分类模型测试完成！")
    print("="*80)

if __name__ == "__main__":
    main()