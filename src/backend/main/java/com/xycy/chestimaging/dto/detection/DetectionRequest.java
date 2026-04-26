package com.xycy.chestimaging.dto.detection;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public class DetectionRequest {
    private Long detectionId;
    private Long caseId;
    private Long modelId;
    private Long imageId;
    @DecimalMin(value = "0.1", message = "置信度阈值不能小于0.1")
    @DecimalMax(value = "0.9", message = "置信度阈值不能大于0.9")
    private Float confidenceThreshold;

    public Long getDetectionId() {
        return detectionId;
    }

    public void setDetectionId(Long detectionId) {
        this.detectionId = detectionId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public Float getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(Float confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }
}