package com.xycy.chestimaging.dto.cases;

import java.time.LocalDateTime;

public class CaseListResponse {
    private Long id;
    private String caseId;
    private String patientName;
    private LocalDateTime createdAt;
    private int imageCount;
    private String status;
    

    public CaseListResponse() {
    }

    public CaseListResponse(Long id, String caseId, String patientName, LocalDateTime createdAt, int imageCount, String status) {
        this.id = id;
        this.caseId = caseId;
        this.patientName = patientName;
        this.createdAt = createdAt;
        this.imageCount = imageCount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}