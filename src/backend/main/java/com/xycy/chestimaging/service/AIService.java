package com.xycy.chestimaging.service;

import com.xycy.chestimaging.dto.detection.DetectionResponse;

import java.io.File;

public interface AIService {
    /**
     * 使用 ResNet 模型进行肺炎分类（只判断有无病灶）
     */
    DetectionResponse.DetectionResult predict(File imageFile);
    
    /**
     * 使用 YOLO 模型进行肺炎病灶检测（返回真实位置）
     */
    DetectionResponse.DetectionResult detectWithYolo(File imageFile);
    
    /**
     * 使用指定模型进行肺炎分类
     */
    DetectionResponse.DetectionResult predict(File imageFile, Long modelId);
    
    /**
     * 使用指定模型进行肺炎病灶检测
     */
    DetectionResponse.DetectionResult detectWithYolo(File imageFile, Long modelId);
    
    /**
     * 使用指定模型和置信度阈值进行肺炎病灶检测
     */
    DetectionResponse.DetectionResult detectWithYolo(File imageFile, Long modelId, float confidenceThreshold);
}
