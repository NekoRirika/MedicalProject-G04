import os
import cv2
import pydicom
import pandas as pd
import numpy as np
from tqdm import tqdm
import shutil

DATASET_ROOT = "./dataset"
YOLO_ROOT = "./yolo_dataset" 
IMG_SIZE = 640

ENABLE_SMALL_LESION_AUG = True
SMALL_LESION_AREA_THRESHOLD = 1024
SMALL_LESION_COPY_TIMES = 2
SMALL_LESION_MAX_OFFSET = 80

def create_dirs():
    dirs = [
        os.path.join(YOLO_ROOT, "images", "train"),
        os.path.join(YOLO_ROOT, "images", "val"),
        os.path.join(YOLO_ROOT, "labels", "train"),
        os.path.join(YOLO_ROOT, "labels", "val"),
    ]
    for d in dirs:
        os.makedirs(d, exist_ok=True)
    print(f"YOLO数据集目录已创建：{YOLO_ROOT}")

def dicom_to_jpg(dicom_path, jpg_path):
    try:
        dicom = pydicom.dcmread(dicom_path, force=True)
        img = dicom.pixel_array.astype(np.float32)
        if img.max() == img.min():
            img = np.zeros_like(img)
        else:
            img = (img - img.min()) / (img.max() - img.min()) * 255
        img = img.astype(np.uint8)
        img = cv2.resize(img, (IMG_SIZE, IMG_SIZE))
        cv2.imwrite(jpg_path, img)
        return True
    except Exception as e:
        print(f"转换失败: {dicom_path} | {e}")
        return False

def convert_annotation(row, original_h, original_w, label_path, enable_aug=True):
    target = row['Target']
    if target != 1:
        return  
    
    bboxes = []
    small_lesions = []
    
    for x, y, w, h in zip(row['x'], row['y'], row['width'], row['height']):
        if x == 0 and y == 0 and w == 0 and h == 0:
            continue
        
        x1 = x
        y1 = y
        x2 = x + w
        y2 = y + h
        
        x_center = ((x1 + x2) / 2) / original_w
        y_center = ((y1 + y2) / 2) / original_h
        norm_w = (x2 - x1) / original_w
        norm_h = (y2 - y1) / original_h
        
        x_center = max(0, min(1, x_center))
        y_center = max(0, min(1, y_center))
        norm_w = max(0, min(1, norm_w))
        norm_h = max(0, min(1, norm_h))
        
        bboxes.append(f"0 {x_center:.6f} {y_center:.6f} {norm_w:.6f} {norm_h:.6f}")
        
        if enable_aug and ENABLE_SMALL_LESION_AUG and (w * h) < SMALL_LESION_AREA_THRESHOLD:
            small_lesions.append((x, y, w, h))
    
    if enable_aug and ENABLE_SMALL_LESION_AUG and small_lesions:
        for _ in range(SMALL_LESION_COPY_TIMES):
            for (x, y, w, h) in small_lesions:
                dx = np.random.randint(-SMALL_LESION_MAX_OFFSET, SMALL_LESION_MAX_OFFSET)
                dy = np.random.randint(-SMALL_LESION_MAX_OFFSET, SMALL_LESION_MAX_OFFSET)
                
                new_x = x + dx
                new_y = y + dy
                
                new_x = max(0, min(original_w - w, new_x))
                new_y = max(0, min(original_h - h, new_y))
                
                new_x1 = new_x
                new_y1 = new_y
                new_x2 = new_x + w
                new_y2 = new_y + h
                
                new_x_center = ((new_x1 + new_x2) / 2) / original_w
                new_y_center = ((new_y1 + new_y2) / 2) / original_h
                new_norm_w = (new_x2 - new_x1) / original_w
                new_norm_h = (new_y2 - new_y1) / original_h
                
                new_x_center = max(0, min(1, new_x_center))
                new_y_center = max(0, min(1, new_y_center))
                new_norm_w = max(0, min(1, new_norm_w))
                new_norm_h = max(0, min(1, new_norm_h))
                
                bboxes.append(f"0 {new_x_center:.6f} {new_y_center:.6f} {new_norm_w:.6f} {new_norm_h:.6f}")
    
    if bboxes:
        with open(label_path, 'w') as f:
            f.write('\n'.join(bboxes))

def main():
    create_dirs()
    
    label_df = pd.read_csv(os.path.join(DATASET_ROOT, "stage_2_train_labels.csv"))
    label_df[['x', 'y', 'width', 'height']] = label_df[['x', 'y', 'width', 'height']].fillna(0)
    
    df_grouped = label_df.groupby('patientId').agg({
        'x': list, 'y': list, 'width': list, 'height': list,
        'Target': "max"
    }).reset_index()
    
    print(f"原始数据总数：{len(df_grouped)}")
    df_lesion_only = df_grouped[df_grouped['Target'] == 1].reset_index(drop=True)
    print(f"有病灶数据总数：{len(df_lesion_only)}")
    
    from sklearn.model_selection import train_test_split
    train_df, val_df = train_test_split(df_lesion_only, test_size=0.2, random_state=42)
    print(f"训练集：{len(train_df)} | 验证集：{len(val_df)}")
    
    print("\n开始处理训练集...")
    img_dir = os.path.join(DATASET_ROOT, "stage_2_train_images")
    for idx, row in tqdm(train_df.iterrows(), total=len(train_df)):
        pid = row['patientId']
        dicom_path = os.path.join(img_dir, f"{pid}.dcm")
        jpg_path = os.path.join(YOLO_ROOT, "images", "train", f"{pid}.jpg")
        label_path = os.path.join(YOLO_ROOT, "labels", "train", f"{pid}.txt")
        
        try:
            dicom = pydicom.dcmread(dicom_path, force=True)
            orig_h, orig_w = dicom.pixel_array.shape
        except:
            continue
        
        if dicom_to_jpg(dicom_path, jpg_path):
            convert_annotation(row, orig_h, orig_w, label_path, enable_aug=True)
    
    print("\n开始处理验证集...")
    for idx, row in tqdm(val_df.iterrows(), total=len(val_df)):
        pid = row['patientId']
        dicom_path = os.path.join(img_dir, f"{pid}.dcm")
        jpg_path = os.path.join(YOLO_ROOT, "images", "val", f"{pid}.jpg")
        label_path = os.path.join(YOLO_ROOT, "labels", "val", f"{pid}.txt")
        
        try:
            dicom = pydicom.dcmread(dicom_path, force=True)
            orig_h, orig_w = dicom.pixel_array.shape
        except:
            continue
        
        if dicom_to_jpg(dicom_path, jpg_path):
            convert_annotation(row, orig_h, orig_w, label_path, enable_aug=False)
    
    yaml_content = f"""
path: {os.path.abspath(YOLO_ROOT)}
train: images/train
val: images/val

names:
  0: lesion
"""
    yaml_path = "rsna_yolo.yaml"
    with open(yaml_path, 'w') as f:
        f.write(yaml_content.strip())
    
    print(f"\n数据转换完成！")
    print(f"   - YOLO数据集：{YOLO_ROOT}")
    print(f"   - YOLO配置文件：{yaml_path}")

if __name__ == "__main__":
    main()