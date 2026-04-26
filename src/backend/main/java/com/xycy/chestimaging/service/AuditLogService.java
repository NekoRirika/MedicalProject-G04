package com.xycy.chestimaging.service;

import com.xycy.chestimaging.dto.PaginationResponse;
import com.xycy.chestimaging.dto.audit.AuditLogResponse;

public interface AuditLogService {
    PaginationResponse<AuditLogResponse> getAuditLogs(int page, int pageSize, String operator, String operationType, String startTime, String endTime);
    AuditLogResponse getAuditLogById(Long id);
    void createAuditLog(String operator, String operationType, String operationContent, String ipAddress, String status, String details);
}
