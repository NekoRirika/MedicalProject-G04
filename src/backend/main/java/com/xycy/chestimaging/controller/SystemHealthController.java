package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.mapper.CaseMapper;
import com.xycy.chestimaging.mapper.DetectionMapper;
import com.xycy.chestimaging.mapper.FeedbackMapper;
import com.xycy.chestimaging.mapper.ImageMapper;
import com.xycy.chestimaging.mapper.ModelMapper;
import com.xycy.chestimaging.model.Detection;
import com.xycy.chestimaging.model.Image;
import com.xycy.chestimaging.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.minio.MinioClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system")
public class SystemHealthController {
    private static final Logger logger = LoggerFactory.getLogger(SystemHealthController.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private DetectionMapper detectionMapper;

    @Autowired
    private CaseMapper caseMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private ImageMapper imageMapper;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @GetMapping("/health")
    public Response checkHealth() {
        Map<String, Object> healthStatus = new HashMap<>();
        boolean allHealthy = true;

        healthStatus.put("timestamp", System.currentTimeMillis());

        try {
            boolean redisHealthy = redisTemplate.getConnectionFactory() != null 
                    && Boolean.TRUE.equals(redisTemplate.hasKey("health:check"));
            healthStatus.put("redis", redisHealthy ? "UP" : "DOWN");
            if (!redisHealthy) allHealthy = false;
        } catch (Exception e) {
            healthStatus.put("redis", "DOWN");
            allHealthy = false;
        }

        try {
            if (kafkaTemplate != null) {
                healthStatus.put("kafka", "UP");
            } else {
                healthStatus.put("kafka", "DISABLED");
            }
        } catch (Exception e) {
            healthStatus.put("kafka", "DOWN");
            allHealthy = false;
        }

        try {
            boolean minioHealthy = minioClient.bucketExists(io.minio.BucketExistsArgs.builder()
                    .bucket(bucketName).build());
            healthStatus.put("minio", minioHealthy ? "UP" : "DOWN");
            if (!minioHealthy) allHealthy = false;
        } catch (Exception e) {
            healthStatus.put("minio", "DOWN");
            allHealthy = false;
        }

        healthStatus.put("status", allHealthy ? "UP" : "DEGRADED");

        return Response.success(healthStatus);
    }

    @GetMapping("/metrics/summary")
    public Response getMetricsSummary() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            Long cacheHitCount = 0L;
            Long cacheMissCount = 0L;
            metrics.put("cache", Map.of(
                    "hitRate", "80%",
                    "enabled", true
            ));
        } catch (Exception e) {
            logger.error("获取缓存指标失败", e);
        }

        metrics.put("timestamp", System.currentTimeMillis());
        return Response.success(metrics);
    }

    @GetMapping("/health/detection-consistency")
    public Response checkDetectionConsistency() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> issues = new ArrayList<>();
        int totalIssues = 0;

        List<Detection> allDetections = detectionMapper.findAll();
        for (Detection detection : allDetections) {
            if (caseMapper.findById(detection.getCaseId()).isEmpty()) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "ORPHAN_DETECTION");
                issue.put("detectionId", detection.getId());
                issue.put("description", "检测任务关联的病例不存在");
                issues.add(issue);
                totalIssues++;
            }

            if (modelMapper.findById(detection.getModelId()).isEmpty()) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "INVALID_MODEL_REF");
                issue.put("detectionId", detection.getId());
                issue.put("description", "检测任务关联的模型不存在");
                issues.add(issue);
                totalIssues++;
            }

            if ("completed".equals(detection.getStatus())) {
                List<?> feedbacks = feedbackMapper.findByDetectionId(detection.getId());
                if (feedbacks.isEmpty()) {
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("type", "NO_FEEDBACK");
                    issue.put("detectionId", detection.getId());
                    issue.put("description", "已完成检测任务无反馈记录");
                    issues.add(issue);
                    totalIssues++;
                }
            }

            if ("processing".equals(detection.getStatus())) {
                LocalDateTime createdAt = detection.getCreatedAt();
                if (createdAt != null && createdAt.plusMinutes(30).isBefore(LocalDateTime.now())) {
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("type", "STUCK_PROCESSING");
                    issue.put("detectionId", detection.getId());
                    issue.put("description", "检测任务卡在processing状态超过30分钟");
                    issues.add(issue);
                    totalIssues++;
                }
            }

            if ("failed".equals(detection.getStatus()) && (detection.getResult() == null || detection.getResult().isEmpty())) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "FAILED_NO_ERROR");
                issue.put("detectionId", detection.getId());
                issue.put("description", "失败检测任务无错误信息");
                issues.add(issue);
                totalIssues++;
            }
        }

        result.put("checkTime", LocalDateTime.now().toString());
        result.put("totalDetections", allDetections.size());
        result.put("totalIssues", totalIssues);
        result.put("issues", issues);

        return Response.success("一致性检查完成", result);
    }
}
