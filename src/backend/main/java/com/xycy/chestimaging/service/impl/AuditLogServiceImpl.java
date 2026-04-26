package com.xycy.chestimaging.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xycy.chestimaging.dto.PaginationResponse;
import com.xycy.chestimaging.dto.audit.AuditLogResponse;
import com.xycy.chestimaging.mapper.AuditLogMapper;
import com.xycy.chestimaging.model.AuditLog;
import com.xycy.chestimaging.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private static final String AUDIT_LOG_QUEUE_KEY = "audit:log:queue";
    private static final int BATCH_SIZE = 100;
    private static final int MAX_QUEUE_LENGTH = 10000;

    @Autowired
    private AuditLogMapper auditLogMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaginationResponse<AuditLogResponse> getAuditLogs(int page, int pageSize, String operator, String operationType, String startTime, String endTime) {
        int offset = (page - 1) * pageSize;
        
        List<AuditLog> auditLogs = auditLogMapper.findByCondition(operator, operationType, startTime, endTime, offset, pageSize);
        long total = auditLogMapper.countByCondition(operator, operationType, startTime, endTime);
        
        List<AuditLogResponse> auditLogResponses = auditLogs.stream()
                .map(AuditLogResponse::new)
                .collect(Collectors.toList());

        return new PaginationResponse<>(total, auditLogResponses);
    }

    @Override
    public AuditLogResponse getAuditLogById(Long id) {
        Optional<AuditLog> optionalAuditLog = auditLogMapper.findById(id);
        if (!optionalAuditLog.isPresent()) {
            throw new RuntimeException("日志不存在");
        }
        return new AuditLogResponse(optionalAuditLog.get());
    }

    @Override
    public void createAuditLog(String operator, String operationType, String operationContent, String ipAddress, String status, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOperationTime(LocalDateTime.now());
        auditLog.setOperator(operator);
        auditLog.setOperationType(operationType);
        auditLog.setOperationContent(operationContent);
        auditLog.setIpAddress(ipAddress);
        auditLog.setStatus(status);
        auditLog.setDetails(details);

        try {
            String logJson = objectMapper.writeValueAsString(auditLog);
            Long queueLength = redisTemplate.opsForList().rightPush(AUDIT_LOG_QUEUE_KEY, logJson);
            
            if (queueLength != null && queueLength > MAX_QUEUE_LENGTH) {
                logger.warn("[审计日志] Redis队列积压过多, 当前长度: {}, 建议检查数据库写入", queueLength);
            }
            
            logger.debug("[审计日志] 已写入Redis队列, 操作人: {}, 操作类型: {}, 队列长度: {}", 
                    operator, operationType, queueLength);
        } catch (JsonProcessingException e) {
            logger.error("[审计日志] 序列化审计日志失败, 直接写入数据库: {}", e.getMessage(), e);
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            logger.error("[审计日志] 写入Redis队列失败, 降级为直接写入数据库: {}", e.getMessage(), e);
            auditLogMapper.insert(auditLog);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void flushAuditLogsToDatabase() {
        try {
            List<AuditLog> batchToInsert = new ArrayList<>(BATCH_SIZE);
            
            for (int i = 0; i < BATCH_SIZE; i++) {
                Object logObj = redisTemplate.opsForList().leftPop(AUDIT_LOG_QUEUE_KEY);
                if (logObj == null) {
                    break;
                }
                
                try {
                    String logJson = logObj.toString();
                    AuditLog auditLog = objectMapper.readValue(logJson, AuditLog.class);
                    batchToInsert.add(auditLog);
                } catch (Exception e) {
                    logger.error("[审计日志] 解析队列中的日志失败: {}", e.getMessage());
                }
            }
            
            if (!batchToInsert.isEmpty()) {
                logger.info("[审计日志] 开始批量刷入数据库, 数量: {}", batchToInsert.size());
                
                for (AuditLog auditLog : batchToInsert) {
                    auditLogMapper.insert(auditLog);
                }
                
                logger.info("[审计日志] 批量刷入数据库完成, 成功: {} 条", batchToInsert.size());
            }
        } catch (Exception e) {
            logger.error("[审计日志] 批量刷入数据库失败: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void flushRemainingLogsOnShutdown() {
        logger.info("[审计日志] 应用关闭, 开始刷入剩余日志...");
        try {
            while (true) {
                Object logObj = redisTemplate.opsForList().leftPop(AUDIT_LOG_QUEUE_KEY);
                if (logObj == null) {
                    break;
                }
                
                try {
                    String logJson = logObj.toString();
                    AuditLog auditLog = objectMapper.readValue(logJson, AuditLog.class);
                    auditLogMapper.insert(auditLog);
                } catch (Exception e) {
                    logger.error("[审计日志] 刷入单条日志失败: {}", e.getMessage());
                }
            }
            logger.info("[审计日志] 剩余日志刷入完成");
        } catch (Exception e) {
            logger.error("[审计日志] 刷入剩余日志失败: {}", e.getMessage(), e);
        }
    }
}
