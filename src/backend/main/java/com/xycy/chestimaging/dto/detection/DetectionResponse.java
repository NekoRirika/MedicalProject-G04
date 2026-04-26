package com.xycy.chestimaging.dto.detection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 加上这一行！忽略不存在的字段
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetectionResponse {
    private Long detectionId;
    private Long caseId;
    private Long modelId;
    private String modelName;
    private String status;
    private List<DetectionResult> imageResults;
    private Map<String, Object> result;  // 整体的检测结果，包含 analysis
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<DetectionResult> getImageResults() {
        return imageResults;
    }

    public void setImageResults(List<DetectionResult> imageResults) {
        this.imageResults = imageResults;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    public static class DetectionResult {
        private Long imageId;
        private String imageName;
        private List<DetectionItem> detections;
        private String analysis;

        public Long getImageId() {
            return imageId;
        }

        public void setImageId(Long imageId) {
            this.imageId = imageId;
        }

        public String getImageName() {
            return imageName;
        }

        public void setImageName(String imageName) {
            this.imageName = imageName;
        }

        public List<DetectionItem> getDetections() {
            return detections;
        }

        public void setDetections(List<DetectionItem> detections) {
            this.detections = detections;
        }

        public String getAnalysis() {
            return analysis;
        }

        public void setAnalysis(String analysis) {
            this.analysis = analysis;
        }
    }

    public static class DetectionItem {
        private double x;
        private double y;
        private double width;
        private double height;
        private double confidence;
        private String label;

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getWidth() {
            return width;
        }

        public void setWidth(double width) {
            this.width = width;
        }

        public double getHeight() {
            return height;
        }

        public void setHeight(double height) {
            this.height = height;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}