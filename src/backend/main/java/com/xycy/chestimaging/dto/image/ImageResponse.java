package com.xycy.chestimaging.dto.image;

import com.xycy.chestimaging.model.Image;

import java.time.LocalDateTime;

public class ImageResponse {
    private Long imageId;
    private Long caseId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String uploadedBy;

    public ImageResponse() {
    }

    public ImageResponse(Image image) {
        this.imageId = image.getId();
        this.caseId = image.getCaseId();
        this.fileName = image.getFileName();
        this.filePath = image.getFilePath();
        this.fileSize = image.getFileSize();
        this.uploadedAt = image.getUploadedAt();
        this.uploadedBy = image.getUploadedBy();
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}