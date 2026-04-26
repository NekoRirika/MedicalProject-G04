package com.xycy.chestimaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xycy.chestimaging.dto.detection.DetectionRequest;
import com.xycy.chestimaging.dto.detection.DetectionResponse;
import com.xycy.chestimaging.dto.image.ImageResponse;
import com.xycy.chestimaging.exception.NotFoundException;
import com.xycy.chestimaging.mapper.DetectionMapper;
import com.xycy.chestimaging.model.Detection;
import com.xycy.chestimaging.utils.FileUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class KafkaConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private DetectionMapper detectionMapper;

    @Autowired
    private AIService aiService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private CaseService caseService;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.detection-result}")
    private String detectionResultTopic;

    @KafkaListener(topics = "${kafka.topic.detection-request}", groupId = "detection-consumer-group")
    public void handleDetectionRequest(ConsumerRecord<String, Object> record) {
        logger.info("[Kafka消费者] ========== 收到检测请求 ==========");
        logger.info("[Kafka消费者] key={}, partition={}, offset={}", 
                record.key(), record.partition(), record.offset());
        logger.info("[Kafka消费者] 消息类型: {}", record.value().getClass().getName());
        logger.info("[Kafka消费者] 消息内容: {}", record.value());
        
        try {
            DetectionRequest request = convertToDetectionRequest(record.value());
            if (request == null) {
                logger.error("[Kafka消费者] 无法解析检测请求消息");
                return;
            }

            Long detectionId = request.getDetectionId();
            Long caseId = request.getCaseId();
            
            logger.info("[Kafka消费者] 解析后的请求: detectionId={}, caseId={}, modelId={}, imageId={}, confidenceThreshold={}", 
                    detectionId, caseId, request.getModelId(), request.getImageId(), request.getConfidenceThreshold());

            Detection detection = detectionMapper.findById(detectionId)
                    .orElseThrow(() -> new RuntimeException("检测任务不存在"));
            
            detection.setStatus("processing");
            detectionMapper.update(detection);

            List<ImageResponse> images = getImagesForDetection(request, caseId);
            if (images.isEmpty()) {
                throw new RuntimeException("病例没有上传任何图像");
            }

            DetectionResponse detectionResponse = executeDetection(images, request.getModelId(), request.getConfidenceThreshold(), detectionId);

            String resultJson = objectMapper.writeValueAsString(detectionResponse);
            detection.setStatus("completed");
            detection.setResult(resultJson);
            detection.setCompletedAt(LocalDateTime.now());
            detectionMapper.update(detection);

            cacheService.cacheDetectionResult(detectionId, detectionResponse);
            cacheService.evictCaseDetailCache(caseId);
            caseService.updateCaseStatus(caseId, "已检测");

            kafkaTemplate.send(detectionResultTopic, "result:" + detectionId, detectionResponse);

            modelService.recalculateModelAccuracy(request.getModelId());
            
            logger.info("[Kafka消费者] 检测任务完成: detectionId={}", detectionId);

        } catch (Exception e) {
            logger.error("[Kafka消费者] 检测任务执行失败: {}", e.getMessage(), e);
            handleDetectionError(record, e);
        }
    }

    private DetectionRequest convertToDetectionRequest(Object value) {
        if (value instanceof DetectionRequest) {
            return (DetectionRequest) value;
        }
        
        try {
            if (value instanceof Map) {
                return objectMapper.convertValue(value, DetectionRequest.class);
            }
            return objectMapper.readValue(value.toString(), DetectionRequest.class);
        } catch (JsonProcessingException e) {
            logger.error("[Kafka消费者] 转换检测请求失败: {}", e.getMessage());
            return null;
        }
    }

    private List<ImageResponse> getImagesForDetection(DetectionRequest request, Long caseId) {
        List<ImageResponse> images = new ArrayList<>();
        if (request.getImageId() != null) {
            ImageResponse image = imageService.getImageById(request.getImageId());
            if (image != null && image.getCaseId().equals(caseId)) {
                images.add(image);
            } else {
                throw new RuntimeException("指定的图像不存在或不属于该病例");
            }
        } else {
            images = imageService.getImagesByCaseId(caseId);
        }
        return images;
    }

    private DetectionResponse executeDetection(List<ImageResponse> images, Long modelId, Float confidenceThreshold, Long detectionId) {
        List<DetectionResponse.DetectionResult> imageResults = new ArrayList<>();
        List<DetectionResponse.DetectionItem> allDetections = new ArrayList<>();
        List<String> failedImages = new ArrayList<>();
        
        float threshold = confidenceThreshold != null ? confidenceThreshold : 0.55f;
        
        if (threshold < 0.1f || threshold > 0.9f) {
            String errorMsg = String.format("置信度阈值必须在 0.1 ~ 0.9 之间，当前值：%.2f", threshold);
            logger.error("[Kafka消费者] {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        
        List<CompletableFuture<DetectionResponse.DetectionResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < images.size(); i++) {
            final ImageResponse image = images.get(i);
            final int imageIndex = i;
            
            CompletableFuture<DetectionResponse.DetectionResult> future = CompletableFuture.supplyAsync(() -> {
                logger.info("[Kafka消费者] 并行处理第 {}/{} 张图像", imageIndex + 1, images.size());
                
                File imageFile = fileUtils.getFile(image.getFilePath());
                if (!imageFile.exists()) {
                    String errorMsg = String.format("图像文件不存在: %s", image.getFilePath());
                    logger.error("[Kafka消费者] {}", errorMsg);
                    throw new RuntimeException(errorMsg);
                }

                DetectionResponse.DetectionResult result = aiService.detectWithYolo(imageFile, modelId, threshold);
                if (result != null) {
                    result.setImageId(image.getImageId());
                    result.setImageName(image.getFileName());
                }
                return result;
            });
            
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<DetectionResponse.DetectionResult> future = futures.get(i);
            try {
                DetectionResponse.DetectionResult result = future.get();
                if (result != null) {
                    imageResults.add(result);
                    if (result.getDetections() != null) {
                        allDetections.addAll(result.getDetections());
                    }
                }
            } catch (Exception e) {
                String imageName = images.get(i).getFileName();
                String errorMsg = String.format("图像[%s]检测失败: %s", imageName, e.getMessage());
                logger.error("[Kafka消费者] {}", errorMsg, e);
                failedImages.add(errorMsg);
            }
        }

        String overallAnalysis = generateAnalysis(allDetections);
        
        DetectionResponse detectionResponse = new DetectionResponse();
        detectionResponse.setImageResults(imageResults);
        
        Map<String, Object> result = new HashMap<>();
        result.put("analysis", overallAnalysis);
        result.put("totalDetections", allDetections.size());
        result.put("totalImages", imageResults.size());
        result.put("failedImages", failedImages);
        result.put("failedCount", failedImages.size());
        detectionResponse.setResult(result);
        
        if (!images.isEmpty()) {
            detectionResponse.setCaseId(images.get(0).getCaseId());
        }
        detectionResponse.setDetectionId(detectionId);
        detectionResponse.setModelId(modelId);
        detectionResponse.setModelName(modelService.getModelNameById(modelId));
        detectionResponse.setStatus("completed");
        detectionResponse.setCreatedAt(LocalDateTime.now());
        detectionResponse.setCompletedAt(LocalDateTime.now());
        
        if (!failedImages.isEmpty()) {
            logger.warn("[Kafka消费者] 检测完成但有{}张图像处理失败", failedImages.size());
        }
        
        return detectionResponse;
    }

    private String generateAnalysis(List<DetectionResponse.DetectionItem> detections) {
        if (detections.isEmpty()) {
            return "AI模型未检测到任何病灶。建议结合临床症状进行进一步诊断。";
        }

        int count = detections.size();
        StringBuilder analysis = new StringBuilder();
        analysis.append("AI模型检测到").append(count).append("处病灶，");

        Map<String, Integer> labelCount = new HashMap<>();
        for (DetectionResponse.DetectionItem item : detections) {
            labelCount.put(item.getLabel(), labelCount.getOrDefault(item.getLabel(), 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : labelCount.entrySet()) {
            analysis.append(entry.getValue()).append("处").append(entry.getKey()).append("，");
        }

        double avgConfidence = detections.stream()
                .mapToDouble(DetectionResponse.DetectionItem::getConfidence)
                .average()
                .orElse(0);
        analysis.append("平均置信度为").append(String.format("%.1f", avgConfidence * 100)).append("%。");
        analysis.append("建议结合临床症状进行进一步诊断。");

        return analysis.toString();
    }

    private void handleDetectionError(ConsumerRecord<String, Object> record, Exception e) {
        try {
            DetectionRequest request = convertToDetectionRequest(record.value());
            if (request != null && request.getDetectionId() != null) {
                Detection detection = detectionMapper.findById(request.getDetectionId()).orElse(null);
                if (detection != null) {
                    detection.setStatus("failed");
                    
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "未知错误";
                    String errorType = classifyError(e);
                    
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", true);
                    errorMap.put("errorType", errorType);
                    errorMap.put("errorMessage", errorMessage);
                    errorMap.put("message", "检测失败：" + errorMessage);
                    errorMap.put("suggestion", getSuggestionForError(errorType));
                    
                    if (e.getCause() != null) {
                        errorMap.put("cause", e.getCause().getMessage());
                    }
                    
                    detection.setResult(objectMapper.writeValueAsString(errorMap));
                    detection.setCompletedAt(LocalDateTime.now());
                    detectionMapper.update(detection);
                    
                    logger.info("[Kafka消费者] 检测任务失败状态已更新: detectionId={}, errorType={}", 
                            request.getDetectionId(), errorType);
                }
            }
        } catch (Exception ex) {
            logger.error("[Kafka消费者] 更新检测失败状态时出错", ex);
        }
    }

    private String classifyError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (message.contains("图像文件不存在") || message.contains("file not found")) {
            return "FILE_NOT_FOUND";
        } else if (message.contains("ai服务") || message.contains("connection") || message.contains("timeout")) {
            return "AI_SERVICE_UNAVAILABLE";
        } else if (message.contains("模型") || message.contains("model")) {
            return "MODEL_ERROR";
        } else if (message.contains("检测任务不存在")) {
            return "TASK_NOT_FOUND";
        } else if (message.contains("病例没有上传")) {
            return "NO_IMAGES";
        } else {
            return "UNKNOWN";
        }
    }

    private String getSuggestionForError(String errorType) {
        switch (errorType) {
            case "FILE_NOT_FOUND":
                return "请检查影像文件是否已正确上传，文件路径是否有效";
            case "AI_SERVICE_UNAVAILABLE":
                return "AI推理服务可能不可用，请检查服务状态后重试";
            case "MODEL_ERROR":
                return "模型加载或推理失败，请检查模型文件是否完整";
            case "TASK_NOT_FOUND":
                return "检测任务不存在，请重新创建检测任务";
            case "NO_IMAGES":
                return "该病例没有上传影像文件，请先上传影像";
            default:
                return "请稍后重试，如问题持续存在请联系管理员";
        }
    }
}
