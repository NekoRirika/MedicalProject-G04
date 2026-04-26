package com.xycy.chestimaging.dto.dashboard;

public class DashboardStats {
    private long pendingCases;
    private long todayImages;
    private long todayDetections;
    private double detectionAccuracy;

    public DashboardStats() {
    }

    public DashboardStats(long pendingCases, long todayImages, long todayDetections, double detectionAccuracy) {
        this.pendingCases = pendingCases;
        this.todayImages = todayImages;
        this.todayDetections = todayDetections;
        this.detectionAccuracy = detectionAccuracy;
    }

    public long getPendingCases() {
        return pendingCases;
    }

    public void setPendingCases(long pendingCases) {
        this.pendingCases = pendingCases;
    }

    public long getTodayImages() {
        return todayImages;
    }

    public void setTodayImages(long todayImages) {
        this.todayImages = todayImages;
    }

    public long getTodayDetections() {
        return todayDetections;
    }

    public void setTodayDetections(long todayDetections) {
        this.todayDetections = todayDetections;
    }

    public double getDetectionAccuracy() {
        return detectionAccuracy;
    }

    public void setDetectionAccuracy(double detectionAccuracy) {
        this.detectionAccuracy = detectionAccuracy;
    }
}
