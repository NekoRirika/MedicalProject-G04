import os
import torch
import torch.nn as nn
import torch.optim as optim
import numpy as np
from tqdm import tqdm
from sklearn.metrics import accuracy_score, classification_report, f1_score, roc_auc_score
import warnings
warnings.filterwarnings('ignore')

from dataset import build_classification_dataloaders, CONFIG as DATA_CONFIG
from resnet import resnet18_rsna, resnet34_rsna, resnet50_rsna

TRAIN_CONFIG = {
    "model_name": "resnet50",          
    "epochs": 50,                       
    "lr": 5e-5,                       
    "weight_decay": 1e-4,      
    "device": "cuda" if torch.cuda.is_available() else "cpu",
    "save_dir": "./trained_models_classification",
    "save_freq": 5,
    "patience": 10,           
    "best_metric": "acc",      
    "use_pretrained": True,   
    "freeze_backbone": True,  
}

def load_pretrained_resnet(model_name, img_size=224, num_classes=2):
    if model_name == "resnet18":
        model = resnet18_rsna(img_size=img_size, num_classes=num_classes)
        torch_model = torch.hub.load('pytorch/vision:v0.15.2', 'resnet18', pretrained=True)
        fc_in_channels = 512 * 1
    elif model_name == "resnet34":
        model = resnet34_rsna(img_size=img_size, num_classes=num_classes)
        torch_model = torch.hub.load('pytorch/vision:v0.15.2', 'resnet34', pretrained=True)
        fc_in_channels = 512 * 1
    elif model_name == "resnet50":
        model = resnet50_rsna(img_size=img_size, num_classes=num_classes)
        torch_model = torch.hub.load('pytorch/vision:v0.15.2', 'resnet50', pretrained=True)
        fc_in_channels = 512 * 4
    else:
        raise ValueError(f"不支持的模型：{model_name}")
    
    pretrained_conv1_weight = torch_model.conv1.weight.data
    new_conv1_weight = pretrained_conv1_weight.mean(dim=1, keepdim=True) 
    model.conv1.weight.data = new_conv1_weight
    
    model_dict = model.state_dict()
    pretrained_dict = torch_model.state_dict()
    pretrained_dict = {k: v for k, v in pretrained_dict.items() 
                       if k in model_dict and k not in ["conv1.weight", "fc.weight", "fc.bias"]}
    model_dict.update(pretrained_dict)
    model.load_state_dict(model_dict)
    
    print(f"预训练权重加载完成")
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
        # 梯度裁剪，防止梯度爆炸
        torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=2.0)
        optimizer.step()
        
        # 统计指标
        total_loss += loss.item() * images.size(0)
        pred = torch.argmax(logits, dim=1).cpu().numpy()
        probs = torch.softmax(logits, dim=1)[:, 1].cpu().detach().numpy()
        preds.extend(pred)
        labels.extend(label.cpu().numpy())
        probs_list.extend(probs)
        
        # 更新进度条
        batch_acc = accuracy_score(label.cpu().numpy(), pred)
        pbar.set_postfix({
            "loss": loss.item(),
            "batch_acc": batch_acc
        })
    
    # 计算本轮训练指标
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
        
        # 核心修改：直接输出分类logits
        logits = model(images)
        loss = criterion(logits, label)
        
        # 统计指标
        total_loss += loss.item() * images.size(0)
        pred = torch.argmax(logits, dim=1).cpu().numpy()
        probs = torch.softmax(logits, dim=1)[:, 1].cpu().numpy()
        preds.extend(pred)
        labels.extend(label.cpu().numpy())
        probs_list.extend(probs)
        
        # 更新进度条
        batch_acc = accuracy_score(label.cpu().numpy(), pred)
        pbar.set_postfix({
            "loss": loss.item(),
            "batch_acc": batch_acc
        })
    
    # 计算验证指标
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
        # 加载预训练权重
        model = load_pretrained_resnet(
            model_name=TRAIN_CONFIG["model_name"],
            img_size=DATA_CONFIG["img_size"],
            num_classes=2
        )
        model = model.to(device)
        
        if TRAIN_CONFIG["freeze_backbone"]:
            for name, param in model.named_parameters():
                if "layer1" in name or "layer2" in name or "conv1" in name or "bn1" in name:
                    param.requires_grad = False
            trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
            total_params = sum(p.numel() for p in model.parameters())
            print(f"Backbone冻结完成 | 可训练参数：{trainable_params/1e6:.2f}M / 总参数：{total_params/1e6:.2f}M")
    else:
        if TRAIN_CONFIG["model_name"] == "resnet18":
            model = resnet18_rsna(img_size=DATA_CONFIG["img_size"], num_classes=2).to(device)
        elif TRAIN_CONFIG["model_name"] == "resnet34":
            model = resnet34_rsna(img_size=DATA_CONFIG["img_size"], num_classes=2).to(device)
        elif TRAIN_CONFIG["model_name"] == "resnet50":
            model = resnet50_rsna(img_size=DATA_CONFIG["img_size"], num_classes=2).to(device)
        else:
            raise ValueError(f"不支持的模型：{TRAIN_CONFIG['model_name']}")
    
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