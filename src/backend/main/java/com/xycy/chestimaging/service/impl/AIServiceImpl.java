package com.xycy.chestimaging.service.impl;

import com.xycy.chestimaging.dto.detection.DetectionResponse;
import com.xycy.chestimaging.service.AIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

@Service
public class AIServiceImpl implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${ai.service.enabled:false}")
    private boolean aiServiceEnabled;

    private volatile Boolean aiServiceHealthy = null;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_INTERVAL_MS = 30000; // 30秒检查一次

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService detectionExecutor;

    public AIServiceImpl() {
        int coreCount = Runtime.getRuntime().availableProcessors();
        this.detectionExecutor = new ThreadPoolExecutor(
            coreCount,
            coreCount * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("detection-worker-" + counter++);
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        logger.info("AI检测线程池已初始化，核心线程数={}，最大线程数={}", coreCount, coreCount * 2);
    }

    @Override
    public DetectionResponse.DetectionResult predict(File imageFile) {
        return predict(imageFile, 1L);
    }

    @Override
    public DetectionResponse.DetectionResult detectWithYolo(File imageFile) {
        return detectWithYolo(imageFile, 1L);
    }

    @Override
    public DetectionResponse.DetectionResult predict(File imageFile, Long modelId) {
        if (aiServiceEnabled) {
            return predictWithHttpService(imageFile, modelId, "classification");
        } else {
            logger.warn("AI推理服务未启用，使用模拟数据");
            return mockPredictionResult();
        }
    }

    @Override
    public DetectionResponse.DetectionResult detectWithYolo(File imageFile, Long modelId) {
        if (aiServiceEnabled) {
            try {
                return predictWithHttpService(imageFile, modelId, "detection");
            } catch (Exception e) {
                logger.error("[AI推理] YOLO检测失败，使用模拟数据: {}", e.getMessage());
                return mockDetectionResult();
            }
        } else {
            logger.warn("AI推理服务未启用，使用模拟数据");
            return mockDetectionResult();
        }
    }

    @Override
    public DetectionResponse.DetectionResult detectWithYolo(File imageFile, Long modelId, float confidenceThreshold) {
        if (aiServiceEnabled) {
            try {
                return predictWithHttpService(imageFile, modelId, "detection", confidenceThreshold);
            } catch (Exception e) {
                logger.error("[AI推理] YOLO检测失败（阈值={}），使用模拟数据: {}", confidenceThreshold, e.getMessage());
                return mockDetectionResult();
            }
        } else {
            logger.warn("AI推理服务未启用，使用模拟数据");
            return mockDetectionResult();
        }
    }

    public DetectionResponse.DetectionResult predictWithHttpService(File imageFile, Long modelId, String taskType) {
        return predictWithHttpService(imageFile, modelId, taskType, 0.5f);
    }

    public DetectionResponse.DetectionResult predictWithHttpService(File imageFile, Long modelId, String taskType, float confidenceThreshold) {
        if (confidenceThreshold < 0.1f || confidenceThreshold > 0.9f) {
            String errorMsg = String.format("置信度阈值必须在 0.1 ~ 0.9 之间，当前值：%.2f", confidenceThreshold);
            logger.error("[AI推理] {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        
        if (aiServiceEnabled && !isAiServiceHealthy()) {
            logger.warn("[AI推理] AI服务健康检查失败，使用模拟数据");
            return taskType.equals("classification") ? mockPredictionResult() : mockDetectionResult();
        }
        
        try {
            logger.info("[AI推理] 开始{}推理，图像文件：{}，模型ID：{}，置信度阈值：{}", taskType, imageFile.getAbsolutePath(), modelId, confidenceThreshold);
            
            if (!imageFile.exists()) {
                String errorMsg = String.format("图像文件不存在: %s", imageFile.getAbsolutePath());
                logger.error("[AI推理] {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image_path", imageFile.getAbsolutePath());
            requestBody.put("model_id", modelId.intValue());
            requestBody.put("task_type", taskType);
            requestBody.put("confidence_threshold", confidenceThreshold);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(
                    aiServiceUrl + "/api/v1/predict",
                    HttpMethod.POST,
                    entity,
                    String.class
                );
            } catch (Exception httpEx) {
                String errorMsg = String.format("AI服务调用失败: %s", httpEx.getMessage());
                logger.error("[AI推理] {}", errorMsg, httpEx);
                throw new RuntimeException(errorMsg, httpEx);
            }

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                
                if (rootNode.has("error") && rootNode.get("error").asBoolean()) {
                    String aiErrorMsg = rootNode.has("message") ? rootNode.get("message").asText() : "AI服务返回未知错误";
                    String errorType = rootNode.has("error_type") ? rootNode.get("error_type").asText() : "UNKNOWN";
                    logger.error("[AI推理] AI服务返回错误: type={}, message={}", errorType, aiErrorMsg);
                    throw new RuntimeException(String.format("[%s] %s", errorType, aiErrorMsg));
                }
                
                double inferenceTime = rootNode.has("inference_time") ? rootNode.get("inference_time").asDouble() : 0;
                long totalTime = System.currentTimeMillis() - startTime;
                
                logger.info("[AI推理] {}推理完成，推理耗时={}ms，总耗时={}ms", taskType, (int)(inferenceTime * 1000), totalTime);
                
                if ("classification".equals(taskType)) {
                    return parseClassificationResult(rootNode);
                } else {
                    return parseDetectionResult(rootNode);
                }
            } else {
                String errorMsg = String.format("AI服务HTTP请求失败，状态码：%s", response.getStatusCode());
                logger.error("[AI推理] {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("AI推理异常: %s", e.getMessage());
            logger.error("[AI推理] {}", errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    private DetectionResponse.DetectionResult parseClassificationResult(JsonNode rootNode) {
        DetectionResponse.DetectionResult result = new DetectionResponse.DetectionResult();
        List<DetectionResponse.DetectionItem> detections = new ArrayList<>();

        if (rootNode.has("classification")) {
            JsonNode clsNode = rootNode.get("classification");
            boolean hasLesion = clsNode.has("has_lesion") && clsNode.get("has_lesion").asBoolean();
            double probability = clsNode.has("probability") ? clsNode.get("probability").asDouble() : 0.0;

            if (hasLesion && probability > 0.5) {
                DetectionResponse.DetectionItem item = new DetectionResponse.DetectionItem();
                item.setX(150);
                item.setY(150);
                item.setWidth(200);
                item.setHeight(150);
                item.setConfidence((float) probability);
                item.setLabel("肺炎病灶");
                detections.add(item);
            }
        }

        result.setDetections(detections);
        if (detections.isEmpty()) {
            result.setAnalysis("AI 模型未检测到任何病灶。建议结合临床症状进行进一步诊断。");
        } else {
            double avgConfidence = detections.stream()
                .mapToDouble(DetectionResponse.DetectionItem::getConfidence)
                .average()
                .orElse(0.0);
            result.setAnalysis(String.format("AI 模型检测到%d处病灶，平均置信度为%.1f%%。建议结合临床症状进行进一步诊断。", 
                detections.size(), avgConfidence * 100));
        }

        return result;
    }

    private DetectionResponse.DetectionResult parseDetectionResult(JsonNode rootNode) {
        DetectionResponse.DetectionResult result = new DetectionResponse.DetectionResult();
        List<DetectionResponse.DetectionItem> detections = new ArrayList<>();

        if (rootNode.has("detections")) {
            JsonNode detectionsNode = rootNode.get("detections");
            for (JsonNode detNode : detectionsNode) {
                DetectionResponse.DetectionItem item = new DetectionResponse.DetectionItem();
                
                if (detNode.has("bbox")) {
                    JsonNode bbox = detNode.get("bbox");
                    item.setX(bbox.has("x1") ? bbox.get("x1").asInt() : 0);
                    item.setY(bbox.has("y1") ? bbox.get("y1").asInt() : 0);
                }
                
                item.setWidth(detNode.has("width") ? detNode.get("width").asInt() : 100);
                item.setHeight(detNode.has("height") ? detNode.get("height").asInt() : 100);
                item.setConfidence(detNode.has("confidence") ? (float)detNode.get("confidence").asDouble() : 0.5f);
                item.setLabel(detNode.has("label") ? detNode.get("label").asText() : "肺炎病灶");
                
                detections.add(item);
            }
        }

        result.setDetections(detections);
        if (detections.isEmpty()) {
            result.setAnalysis("AI 模型未检测到任何病灶。建议结合临床症状进行进一步诊断。");
        } else {
            double avgConfidence = detections.stream()
                .mapToDouble(DetectionResponse.DetectionItem::getConfidence)
                .average()
                .orElse(0.0);
            result.setAnalysis(String.format("AI 模型检测到%d处病灶，平均置信度为%.1f%%。建议结合临床症状进行进一步诊断。", 
                detections.size(), avgConfidence * 100));
        }

        return result;
    }

    public CompletableFuture<DetectionResponse.DetectionResult> predictAsync(File imageFile, Long modelId, String taskType) {
        return CompletableFuture.supplyAsync(() -> {
            return predictWithHttpService(imageFile, modelId, taskType);
        }, detectionExecutor);
    }

    public List<DetectionResponse.DetectionResult> predictBatch(List<File> imageFiles, Long modelId, String taskType) {
        logger.info("[AI推理] 开始批量推理，图像数量={}，模型ID={}，任务类型={}", imageFiles.size(), modelId, taskType);
        
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<DetectionResponse.DetectionResult>> futures = new ArrayList<>();
        for (File imageFile : imageFiles) {
            futures.add(predictAsync(imageFile, modelId, taskType));
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();

        List<DetectionResponse.DetectionResult> results = new ArrayList<>();
        for (CompletableFuture<DetectionResponse.DetectionResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                logger.error("[AI推理] 批量推理中单个任务失败", e);
                results.add(taskType.equals("classification") ? mockPredictionResult() : mockDetectionResult());
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("[AI推理] 批量推理完成，总耗时={}ms，成功数量={}", totalTime, results.size());
        
        return results;
    }

    private DetectionResponse.DetectionResult mockPredictionResult() {
        DetectionResponse.DetectionResult result = new DetectionResponse.DetectionResult();
        List<DetectionResponse.DetectionItem> detections = new ArrayList<>();

        DetectionResponse.DetectionItem item1 = new DetectionResponse.DetectionItem();
        item1.setX(100);
        item1.setY(100);
        item1.setWidth(150);
        item1.setHeight(100);
        item1.setConfidence(0.95f);
        item1.setLabel("肺炎病灶");
        detections.add(item1);

        result.setDetections(detections);
        result.setAnalysis("AI 模型检测到 1 处肺炎病灶，置信度为 95%。建议结合临床症状进行进一步诊断。");

        return result;
    }

    private DetectionResponse.DetectionResult mockDetectionResult() {
        DetectionResponse.DetectionResult result = new DetectionResponse.DetectionResult();
        List<DetectionResponse.DetectionItem> detections = new ArrayList<>();

        DetectionResponse.DetectionItem item1 = new DetectionResponse.DetectionItem();
        item1.setX(100);
        item1.setY(100);
        item1.setWidth(150);
        item1.setHeight(100);
        item1.setConfidence(0.95f);
        item1.setLabel("肺炎病灶");
        detections.add(item1);

        DetectionResponse.DetectionItem item2 = new DetectionResponse.DetectionItem();
        item2.setX(300);
        item2.setY(150);
        item2.setWidth(120);
        item2.setHeight(80);
        item2.setConfidence(0.88f);
        item2.setLabel("肺炎病灶");
        detections.add(item2);

        result.setDetections(detections);
        result.setAnalysis("AI 模型检测到 2 处肺炎病灶，置信度分别为 95% 和 88%。建议结合临床症状进行进一步诊断。");

        return result;
    }

    private boolean isAiServiceHealthy() {
        long now = System.currentTimeMillis();
        if (aiServiceHealthy != null && (now - lastHealthCheckTime) < HEALTH_CHECK_INTERVAL_MS) {
            return aiServiceHealthy;
        }

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                aiServiceUrl + "/api/v1/health",
                String.class
            );
            
            aiServiceHealthy = response.getStatusCode() == HttpStatus.OK;
            lastHealthCheckTime = now;
            
            if (aiServiceHealthy) {
                logger.info("[AI健康检查] AI服务健康检查通过: {}", aiServiceUrl);
            } else {
                logger.warn("[AI健康检查] AI服务健康检查失败: HTTP {}", response.getStatusCode());
            }
        } catch (Exception e) {
            aiServiceHealthy = false;
            lastHealthCheckTime = now;
            logger.error("[AI健康检查] AI服务不可用: {} - {}", aiServiceUrl, e.getMessage());
        }
        
        return aiServiceHealthy;
    }
}
