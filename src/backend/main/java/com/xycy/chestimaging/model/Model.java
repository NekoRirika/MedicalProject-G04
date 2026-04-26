package com.xycy.chestimaging.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class Model {
    private Long id;
    private String name;
    private String version;
    private String status;
    private Double accuracy;
    private String classificationModelPath;
    private String detectionModelPath;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loadedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public String getClassificationModelPath() {
        return classificationModelPath;
    }

    public void setClassificationModelPath(String classificationModelPath) {
        this.classificationModelPath = classificationModelPath;
    }

    public String getDetectionModelPath() {
        return detectionModelPath;
    }

    public void setDetectionModelPath(String detectionModelPath) {
        this.detectionModelPath = detectionModelPath;
    }

    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }

    public void setLoadedAt(LocalDateTime loadedAt) {
        this.loadedAt = loadedAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }
}