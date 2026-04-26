import os
import cv2
import pydicom
import pandas as pd
import numpy as np
import torch
from torch.utils.data import Dataset, DataLoader
from sklearn.model_selection import train_test_split
from tqdm import tqdm
import albumentations as A

CONFIG = {
    "dataset_root": "./dataset",
    "img_size": 224,
    "batch_size": 32,
    "num_workers": 0,
    "random_seed": 42,
    "sample_num": 1000
}

def get_train_augmentation(img_size):
    return A.Compose([
        A.Resize(height=img_size, width=img_size),
        A.HorizontalFlip(p=0.5),  
        A.ShiftScaleRotate(shift_limit=0.05, scale_limit=0.1, rotate_limit=10, p=0.5), 
        A.RandomBrightnessContrast(brightness_limit=0.15, contrast_limit=0.15, p=0.5), 
        A.GaussNoise(var_limit=(5, 15), p=0.2),  
        A.OneOf([
            A.MedianBlur(blur_limit=3, p=1),
            A.GaussianBlur(blur_limit=3, p=1),
        ], p=0.2), 
        A.Normalize(mean=0.0, std=1.0, max_pixel_value=255.0),  
    ])

def get_val_augmentation(img_size):
    return A.Compose([
        A.Resize(height=img_size, width=img_size),
        A.Normalize(mean=0.0, std=1.0, max_pixel_value=255.0),
    ])

def check_dataset():
    required = [
        os.path.join(CONFIG["dataset_root"], "stage_2_train_images"),
        os.path.join(CONFIG["dataset_root"], "stage_2_train_labels.csv")
    ]
    for p in required:
        if not os.path.exists(p):
            raise FileNotFoundError(f"缺失文件：{p}")

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
        return img.astype(np.uint8)
    except Exception as e:
        print(f"DICOM加载失败: {dicom_path}")
        return np.random.randint(0, 255, (CONFIG["img_size"], CONFIG["img_size"]), dtype=np.uint8)

def calculate_mean_std(train_df, img_dir):
    print(f"\n采样{CONFIG['sample_num']}张训练集图像计算Mean/Std...")
    pixel_sum = 0.0
    pixel_sum_sq = 0.0
    pixel_count = 0.0
    sample_df = train_df.sample(n=min(CONFIG["sample_num"], len(train_df)), random_state=CONFIG["random_seed"])
    
    for idx, row in tqdm(sample_df.iterrows(), total=len(sample_df)):
        pid = row['patientId']
        dicom_path = os.path.join(img_dir, f"{pid}.dcm")
        img = dicom_to_gray(dicom_path)
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

def parse_annotations():
    label_df = pd.read_csv(os.path.join(CONFIG["dataset_root"], "stage_2_train_labels.csv"))
    df_classification = label_df[['patientId', 'Target']].drop_duplicates().reset_index(drop=True)
    
    img_dir = os.path.join(CONFIG["dataset_root"], "stage_2_train_images")
    existing_pids = [f.split(".dcm")[0] for f in os.listdir(img_dir) if f.endswith(".dcm")]
    df_classification = df_classification[df_classification["patientId"].isin(existing_pids)]
    
    train_df, test_df = train_test_split(
        df_classification, test_size=0.2, random_state=CONFIG["random_seed"], stratify=df_classification['Target']
    )
    print(f"数据集划分完成 | 训练集：{len(train_df)} 样本 | 测试集：{len(test_df)} 样本")
    #print(f"训练集类别分布：无病灶 {sum(train_df['Target']==0)} | 有病灶 {sum(train_df['Target']==1)}")
    #print(f"验证集类别分布：无病灶 {sum(test_df['Target']==0)} | 有病灶 {sum(test_df['Target']==1)}")
    return train_df, test_df

class RSNAClassificationDataset(Dataset):
    def __init__(self, df, img_dir, mean, std, is_train=True):
        self.df = df.reset_index(drop=True)
        self.img_dir = img_dir
        self.mean = mean
        self.std = std
        self.is_train = is_train
        # 初始化增强器
        if is_train:
            self.aug = get_train_augmentation(CONFIG["img_size"])
        else:
            self.aug = get_val_augmentation(CONFIG["img_size"])

    def __len__(self):
        return len(self.df)

    def __getitem__(self, idx):
        row = self.df.iloc[idx]
        pid = row['patientId']
        dicom_path = os.path.join(self.img_dir, f"{pid}.dcm")
        img = dicom_to_gray(dicom_path)
        label = torch.tensor(row['Target'], dtype=torch.long)
        
        aug_result = self.aug(image=img)
        img_aug = aug_result['image']
        
        img_normalized = (img_aug - self.mean) / self.std
        img_tensor = torch.from_numpy(img_normalized).unsqueeze(0).float()
        
        return {"image": img_tensor, "label": label}

def build_classification_dataloaders():
    check_dataset()
    train_df, test_df = parse_annotations()
    img_dir = os.path.join(CONFIG["dataset_root"], "stage_2_train_images")
    mean, std = calculate_mean_std(train_df, img_dir)
    
    train_dataset = RSNAClassificationDataset(train_df, img_dir, mean, std, is_train=True)
    test_dataset = RSNAClassificationDataset(test_df, img_dir, mean, std, is_train=False)
    
    train_loader = DataLoader(
        train_dataset, 
        batch_size=CONFIG["batch_size"], 
        shuffle=True, 
        num_workers=CONFIG["num_workers"], 
        pin_memory=True,
        drop_last=True
    )
    test_loader = DataLoader(
        test_dataset, 
        batch_size=CONFIG["batch_size"], 
        shuffle=False, 
        num_workers=CONFIG["num_workers"], 
        pin_memory=True
    )
    
    save_path = os.path.join(CONFIG["dataset_root"], "mean_std.npy")
    np.save(save_path, [mean, std])
    print(f"Mean/Std已保存至：{save_path}")
    return train_loader, test_loader, mean, std