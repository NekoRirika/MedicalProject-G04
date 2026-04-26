import os
import torch
import torch.nn as nn
import torch.optim as optim
import numpy as np
from tqdm import tqdm
from sklearn.metrics import accuracy_score, classification_report, f1_score, roc_auc_score
import warnings
warnings.filterwarnings('ignore')

import timm

from dataset import build_classification_dataloaders, CONFIG as DATA_CONFIG

TRAIN_CONFIG = {
    "model_name": "efficientnetv2_m",  
    "epochs": 50,                       
    "lr": 5e-5,                       
    "weight_decay": 1e-4,      
    "device": "cuda" if torch.cuda.is_available() else "cpu",
    "save_dir": "./trained_models_classification_efficientnetv2",
    "save_freq": 5,
    "patience": 10,           
    "best_metric": "acc",      
    "use_pretrained": True,   
    "freeze_backbone": True,  
}

def load_pretrained_efficientnetv2(model_name, img_size=224, num_classes=2):
    
    timm_model_mapping = {
        "efficientnetv2_s": "tf_efficientnetv2_s.in21k_ft_in1k",
        "efficientnetv2_m": "tf_efficientnetv2_m.in21k_ft_in1k",
        "efficientnetv2_l": "tf_efficientnetv2_l.in21k_ft_in1k",
    }
    
    if model_name not in timm_model_mapping:
        raise ValueError(f"不支持的模型：{model_name}，可选：{list(timm_model_mapping.keys())}")
    
    model = timm.create_model(
        timm_model_mapping[model_name],
        pretrained=True,
        in_chans=1,  
        num_classes=num_classes, 
        drop_rate=0.5, 
        drop_path_rate=0.2 
    )
    
    print(f"EfficientNetV2 预训练权重加载完成 | 模型：{model_name}")
    return model

def setup_seed(seed=DATA_CONFIG["random_seed"]):
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed(seed)
        torch.cuda.manual_seed_all(seed)

def save_model(model, epoch, optimizer, metrics, save_path):
    torch.save({
        "epoch": epoch,
        "model_state_dict": model.state_dict(),
        "optimizer_state_dict": optimizer.state_dict(),
        "metrics": metrics,
        "config": TRAIN_CONFIG,
    }, save_path)
    print(f"模型已保存至：{save_path}")

def train_one_epoch(model, train_loader, criterion, optimizer, device, epoch):
    model.train()
    total_loss = 0.0
    preds = []
    labels = []
    probs_list = []
    
    pbar = tqdm(train_loader, desc=f"Train Epoch {epoch+1}/{TRAIN_CONFIG['epochs']}")
    for batch in pbar:
        images = batch["image"].to(device)
        label = batch["label"].to(device)
        
        logits = model(images)
        loss = criterion(logits, label)
        
        optimizer.zero_grad()
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=2.0)
        optimizer.step()
        
        total_loss += loss.item() * images.size(0)
        pred = torch.argmax(logits, dim=1).cpu().numpy()
        probs = torch.softmax(logits, dim=1)[:, 1].cpu().detach().numpy()
        preds.extend(pred)
        labels.extend(label.cpu().numpy())
        probs_list.extend(probs)
        
        batch_acc = accuracy_score(label.cpu().numpy(), pred)
        pbar.set_postfix({
            "loss": loss.item(),
            "batch_acc": batch_acc
        })
    
    avg_loss = total_loss / len(train_loader.dataset)
    acc = accuracy_score(labels, preds)
    macro_f1 = f1_score(labels, preds, average='macro')
    try:
        auc = roc_auc_score(labels, probs_list)
    except:
        auc = 0.0
    
    return {
        "total_loss": avg_loss,
        "acc": acc,
        "macro_f1": macro_f1,
        "auc": auc
    }

@torch.no_grad()
def validate(model, val_loader, criterion, device):
    model.eval()
    total_loss = 0.0
    preds = []
    labels = []
    probs_list = []
    
    pbar = tqdm(val_loader, desc="Validate (Classification)")
    for batch in pbar:
        images = batch["image"].to(device)
        label = batch["label"].to(device)
        
        logits = model(images)
        loss = criterion(logits, label)
        
        total_loss += loss.item() * images.size(0)
        pred = torch.argmax(logits, dim=1).cpu().numpy()
        probs = torch.softmax(logits, dim=1)[:, 1].cpu().numpy()
        preds.extend(pred)
        labels.extend(label.cpu().numpy())
        probs_list.extend(probs)
        
        batch_acc = accuracy_score(label.cpu().numpy(), pred)
        pbar.set_postfix({
            "loss": loss.item(),
            "batch_acc": batch_acc
        })
    
    avg_loss = total_loss / len(val_loader.dataset)
    acc = accuracy_score(labels, preds)
    macro_f1 = f1_score(labels, preds, average='macro')
    try:
        auc = roc_auc_score(labels, probs_list)
    except:
        auc = 0.0
    
    print("\n分类验证报告：")
    print(classification_report(labels, preds, target_names=["无病灶", "有病灶"], zero_division=0))
    print(f"核心指标 | ACC: {acc:.4f} | Macro F1: {macro_f1:.4f} | AUC: {auc:.4f}")
    
    return {
        "total_loss": avg_loss,
        "acc": acc,
        "macro_f1": macro_f1,
        "auc": auc
    }

def main():
    setup_seed()
    os.makedirs(TRAIN_CONFIG["save_dir"], exist_ok=True)
    device = torch.device(TRAIN_CONFIG["device"])
    print(f"分类任务训练开始 | 设备：{device} | 模型：{TRAIN_CONFIG['model_name']}")
    print(f"训练配置：预训练={TRAIN_CONFIG['use_pretrained']} | 冻结backbone={TRAIN_CONFIG['freeze_backbone']}")
    
    train_loader, val_loader, mean, std = build_classification_dataloaders()
    
    if TRAIN_CONFIG["use_pretrained"]:
        model = load_pretrained_efficientnetv2(
            model_name=TRAIN_CONFIG["model_name"],
            img_size=DATA_CONFIG["img_size"],
            num_classes=2
        )
        model = model.to(device)
        
        if TRAIN_CONFIG["freeze_backbone"]:
            for name, param in model.named_parameters():
                if "stem" in name:
                    param.requires_grad = False
                elif "blocks" in name:
                    block_idx = int(name.split(".")[1])
                    total_blocks = len(model.blocks)
                    if block_idx < int(total_blocks * 2 / 3):
                        param.requires_grad = False
            
            trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
            total_params = sum(p.numel() for p in model.parameters())
            print(f"Backbone冻结完成 | 可训练参数：{trainable_params/1e6:.2f}M / 总参数：{total_params/1e6:.2f}M")
    else:
        model = load_pretrained_efficientnetv2(
            model_name=TRAIN_CONFIG["model_name"],
            img_size=DATA_CONFIG["img_size"],
            num_classes=2
        )
        model = model.to(device)
    
    class_weights = torch.tensor([1.0, 3.5], dtype=torch.float32).to(device)
    criterion = nn.CrossEntropyLoss(weight=class_weights)
    
    trainable_params = list(filter(lambda p: p.requires_grad, model.parameters()))
    optimizer = optim.AdamW(
        trainable_params,
        lr=TRAIN_CONFIG["lr"],
        weight_decay=TRAIN_CONFIG["weight_decay"]
    )
    scheduler = optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=TRAIN_CONFIG["epochs"], eta_min=1e-6)
    
    best_metric_value = 0.0
    early_stop_count = 0
    
    for epoch in range(TRAIN_CONFIG["epochs"]):
        train_metrics = train_one_epoch(model, train_loader, criterion, optimizer, device, epoch)
        val_metrics = validate(model, val_loader, criterion, device)
        scheduler.step()
        
        print(f"\nEpoch {epoch+1} 总结：")
        print(f"训练集 | Loss: {train_metrics['total_loss']:.4f} | Acc: {train_metrics['acc']:.4f} | Macro F1: {train_metrics['macro_f1']:.4f} | AUC: {train_metrics['auc']:.4f}")
        print(f"验证集 | Loss: {val_metrics['total_loss']:.4f} | Acc: {val_metrics['acc']:.4f} | Macro F1: {val_metrics['macro_f1']:.4f} | AUC: {val_metrics['auc']:.4f}")
        print(f"当前学习率：{scheduler.get_last_lr()[0]:.8f}")
        
        if (epoch+1) % TRAIN_CONFIG["save_freq"] == 0:
            save_path = os.path.join(TRAIN_CONFIG["save_dir"], f"checkpoint_epoch_{epoch+1}.pth")
            save_model(model, epoch+1, optimizer, val_metrics, save_path)
        
        current_metric = val_metrics[TRAIN_CONFIG["best_metric"]]
        if current_metric > best_metric_value:
            best_metric_value = current_metric
            best_save_path = os.path.join(TRAIN_CONFIG["save_dir"], "best_classification_model.pth")
            save_model(model, epoch+1, optimizer, val_metrics, best_save_path)
            print(f"最优模型已更新 | 最佳{TRAIN_CONFIG['best_metric']}: {best_metric_value:.4f}")
            early_stop_count = 0
        else:
            early_stop_count += 1
            print(f"早停计数器：{early_stop_count}/{TRAIN_CONFIG['patience']}")
        
        if early_stop_count >= TRAIN_CONFIG["patience"]:
            print(f"\n验证集{TRAIN_CONFIG['best_metric']}连续{TRAIN_CONFIG['patience']}轮无提升，触发早停")
            break
    
    print("\n分类任务训练完成！最优验证集指标：")
    print(f"最佳{TRAIN_CONFIG['best_metric']}：{best_metric_value:.4f}")

if __name__ == "__main__":
    main()