package com.xycy.chestimaging.model;

import java.time.LocalDateTime;

public class Feedback {
    private Long id;
    private Long detectionId;
    private String evaluation;
    private String feedback;
    private LocalDateTime createdAt;
    private String createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDetectionId() {
        return detectionId;
    }

    public void setDetectionId(Long detectionId) {
        this.detectionId = detectionId;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}