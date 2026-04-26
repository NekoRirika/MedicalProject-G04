package com.xycy.chestimaging.service;

import com.xycy.chestimaging.dto.detection.DetectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.detection-request}")
    private String detectionRequestTopic;

    @Value("${kafka.topic.detection-dead-letter}")
    private String deadLetterTopic;

    public CompletableFuture<SendResult<String, Object>> sendDetectionRequest(Long detectionId, Long caseId, DetectionRequest request) {
        String key = "detection:" + detectionId;
        logger.info("[Kafka生产者] 发送检测请求到队列: detectionId={}, caseId={}, topic={}", detectionId, caseId, detectionRequestTopic);
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(detectionRequestTopic, key, request);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("[Kafka生产者] 检测请求发送成功: detectionId={}, partition={}, offset={}", 
                        detectionId, 
                        result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().offset());
            } else {
                logger.error("[Kafka生产者] 检测请求发送失败: detectionId={}, error={}", detectionId, ex.getMessage(), ex);
                sendToDeadLetterQueue(detectionId, request, ex.getMessage());
            }
        });
        
        return future;
    }

    private void sendToDeadLetterQueue(Long detectionId, DetectionRequest request, String errorMessage) {
        logger.warn("[Kafka生产者] 发送消息到死信队列: detectionId={}, error={}", detectionId, errorMessage);
        kafkaTemplate.send(deadLetterTopic, "dead:" + detectionId, request);
    }
}
