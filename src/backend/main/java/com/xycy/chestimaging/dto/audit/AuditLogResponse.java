package com.xycy.chestimaging.dto.audit;

import com.xycy.chestimaging.model.AuditLog;

import java.time.LocalDateTime;

public class AuditLogResponse {
    private Long id;
    private LocalDateTime operationTime;
    private String operator;
    private String operationType;
    private String operationContent;
    private String ipAddress;
    private String status;
    private String details;

    public AuditLogResponse(AuditLog auditLog) {
        this.id = auditLog.getId();
        this.operationTime = auditLog.getOperationTime();
        this.operator = auditLog.getOperator();
        this.operationType = auditLog.getOperationType();
        this.operationContent = auditLog.getOperationContent();
        this.ipAddress = auditLog.getIpAddress();
        this.status = auditLog.getStatus();
        this.details = auditLog.getDetails();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationContent() {
        return operationContent;
    }

    public void setOperationContent(String operationContent) {
        this.operationContent = operationContent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}