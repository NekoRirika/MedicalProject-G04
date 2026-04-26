package com.xycy.chestimaging.dto.image;

import java.util.List;

public class BatchUploadResult {
    private int totalCount;
    private int successCount;
    private int failedCount;
    private List<ImageResponse> successImages;
    private List<String> failedFiles;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<ImageResponse> getSuccessImages() {
        return successImages;
    }

    public void setSuccessImages(List<ImageResponse> successImages) {
        this.successImages = successImages;
    }

    public List<String> getFailedFiles() {
        return failedFiles;
    }

    public void setFailedFiles(List<String> failedFiles) {
        this.failedFiles = failedFiles;
    }
}
