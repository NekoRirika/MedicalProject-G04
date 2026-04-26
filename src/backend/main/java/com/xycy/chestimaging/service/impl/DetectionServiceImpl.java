package com.xycy.chestimaging.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xycy.chestimaging.dto.detection.DetectionRequest;
import com.xycy.chestimaging.dto.detection.DetectionResponse;
import com.xycy.chestimaging.dto.detection.FeedbackListItem;
import com.xycy.chestimaging.dto.detection.FeedbackRequest;
import com.xycy.chestimaging.exception.NotFoundException;
import com.xycy.chestimaging.mapper.DetectionMapper;
import com.xycy.chestimaging.mapper.FeedbackMapper;
import com.xycy.chestimaging.model.Detection;
import com.xycy.chestimaging.model.Feedback;
import com.xycy.chestimaging.dto.image.ImageResponse;
import com.xycy.chestimaging.service.AIService;
import com.xycy.chestimaging.service.CacheService;
import com.xycy.chestimaging.service.CaseService;
import com.xycy.chestimaging.service.DetectionService;
import com.xycy.chestimaging.service.ImageService;
import com.xycy.chestimaging.service.KafkaProducerService;
import com.xycy.chestimaging.service.ModelService;
import com.xycy.chestimaging.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DetectionServiceImpl implements DetectionService {
    private static final Logger logger = LoggerFactory.getLogger(DetectionServiceImpl.class);
    @Autowired
    private DetectionMapper detectionMapper;
    @Autowired
    private FeedbackMapper feedbackMapper;
    @Autowired
    private ModelService modelService;
    @Autowired
    @Lazy
    private CaseService caseService;
    @Autowired
    private AIService aiService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private FileUtils fileUtils;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Override
    public DetectionResponse createDetection(Long caseId, DetectionRequest request) {
        logger.info("[检测流程] ========== 1. 创建检测任务 ==========");
        logger.info("[检测流程] caseId={}, modelId={}, imageId={}", caseId, request.getModelId(), request.getImageId());

        try {
            caseService.getCaseById(caseId);
            logger.info("[检测流程] 病例验证通过");
        } catch (NotFoundException e) {
            logger.error("[检测流程] 病例不存在！caseId={}", caseId);
            throw new NotFoundException("病例不存在");
        }

        Detection detection = new Detection();
        detection.setCaseId(caseId);
        detection.setModelId(request.getModelId());
        detection.setStatus("pending");
        detection.setResult(null);
        detection.setCreatedAt(LocalDateTime.now());
        detection.setCompletedAt(null);

        detectionMapper.insert(detection);
        logger.info("[检测流程] 检测记录已创建, detectionId={}, status=pending", detection.getId());

        logger.info("[检测流程] 发送检测请求到Kafka队列...");
        request.setDetectionId(detection.getId());
        request.setCaseId(caseId);
        kafkaProducerService.sendDetectionRequest(detection.getId(), caseId, request);

        DetectionResponse response = new DetectionResponse();
        response.setDetectionId(detection.getId());
        response.setCaseId(detection.getCaseId());
        response.setModelId(detection.getModelId());
        response.setModelName(modelService.getModelNameById(request.getModelId()));
        response.setStatus(detection.getStatus());
        response.setCreatedAt(detection.getCreatedAt());

        logger.info("[检测流程] 返回响应给前端, detectionId={}", response.getDetectionId());
        return response;
    }

    @Async
    public CompletableFuture<Void> executeDetection(Long detectionId, Long caseId, DetectionRequest request) {
        logger.info("[检测流程] ========== 2. 异步执行检测开始 ==========");
        logger.info("[检测流程] detectionId={}, caseId={}", detectionId, caseId);
        try {
            Detection detection = detectionMapper.findById(detectionId).orElseThrow(() -> new RuntimeException("检测任务不存在"));
            detection.setStatus("processing");
            detectionMapper.update(detection);
            logger.info("[检测流程] 状态更新为 processing");

            List<ImageResponse> images = new ArrayList<>();
            if (request.getImageId() != null) {
                logger.info("[检测流程] 指定了单张图像, imageId={}", request.getImageId());
                ImageResponse image = imageService.getImageById(request.getImageId());
                if (image != null && image.getCaseId().equals(caseId)) {
                    images.add(image);
                    logger.info("[检测流程] 图像验证通过, fileName={}, filePath={}", image.getFileName(), image.getFilePath());
                } else {
                    throw new RuntimeException("指定的图像不存在或不属于该病例");
                }
            } else {
                logger.info("[检测流程] 未指定图像，获取病例所有图像");
                images = imageService.getImagesByCaseId(caseId);
                logger.info("[检测流程] 获取到 {} 张图像", images.size());
                for (ImageResponse img : images) {
                    logger.info("[检测流程]   - imageId={}, fileName={}, filePath={}", img.getImageId(), img.getFileName(), img.getFilePath());
                }
            }
            
            if (images.isEmpty()) {
                logger.error("[检测流程] 病例没有上传任何图像，检测终止！");
                throw new RuntimeException("病例没有上传任何图像，请先上传影像后再进行检测");
            }

            List<DetectionResponse.DetectionResult> imageResults = new ArrayList<>();
            List<DetectionResponse.DetectionItem> allDetections = new ArrayList<>();
            
            logger.info("[检测流程] 开始并行检测 {} 张图像", images.size());
            List<CompletableFuture<DetectionResponse.DetectionResult>> futures = new ArrayList<>();
            final Long modelId = request.getModelId();
            final int totalImages = images.size();
            
            for (int i = 0; i < images.size(); i++) {
                final ImageResponse image = images.get(i);
                final int imageIndex = i;
                
                CompletableFuture<DetectionResponse.DetectionResult> future = CompletableFuture.supplyAsync(() -> {
                    logger.info("[检测流程] ========== 并行处理第 {}/{} 张图像 ==========", imageIndex + 1, totalImages);
                    logger.info("[检测流程] imageId={}, fileName={}", image.getImageId(), image.getFileName());

                    File imageFile = fileUtils.getFile(image.getFilePath());
                    logger.info("[检测流程] 图像文件路径: {}", imageFile.getAbsolutePath());

                    if (!imageFile.exists()) {
                        logger.warn("[检测流程] 图像文件不存在，跳过该图像！");
                        return null;
                    }

                    logger.info("[检测流程] 调用 AI 服务进行 YOLO 检测, modelId={}", modelId);
                    DetectionResponse.DetectionResult result = aiService.detectWithYolo(imageFile, modelId);
                    
                    if (result != null) {
                        result.setImageId(image.getImageId());
                        result.setImageName(image.getFileName());
                        
                        int detCount = (result.getDetections() != null) ? result.getDetections().size() : 0;
                        logger.info("[检测流程] AI检测完成, 检测到 {} 个病灶", detCount);
                        logger.info("[检测流程] 分析结果: {}", result.getAnalysis());
                    } else {
                        logger.error("[检测流程] AI服务返回null结果！");
                    }
                    
                    return result;
                });
                
                futures.add(future);
            }
            
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allFutures.join();
            
            for (CompletableFuture<DetectionResponse.DetectionResult> future : futures) {
                try {
                    DetectionResponse.DetectionResult result = future.get();
                    if (result != null) {
                        imageResults.add(result);
                        if (result.getDetections() != null) {
                            for (DetectionResponse.DetectionItem det : result.getDetections()) {
                                logger.info("[检测流程]   - {}: x={}, y={}, w={}, h={}, conf={}", det.getLabel(), det.getX(), det.getY(), det.getWidth(), det.getHeight(), det.getConfidence());
                            }
                            allDetections.addAll(result.getDetections());
                        }
                    }
                } catch (Exception e) {
                    logger.error("[检测流程] 单个图像检测失败", e);
                }
            }

            logger.info("[检测流程] ========== 所有图像检测完毕 ==========");
            logger.info("[检测流程] 共处理 {} 张图像，总计 {} 个病灶", imageResults.size(), allDetections.size());

            String overallAnalysis = generateAnalysis(allDetections);
            logger.info("[检测流程] 整体分析结果：{}", overallAnalysis);

            // 重新从数据库获取最新的检测记录
            detection = detectionMapper.findById(detectionId).orElseThrow(() -> new RuntimeException("检测任务不存在"));
            
            DetectionResponse detectionResponse = new DetectionResponse();
            detectionResponse.setDetectionId(detection.getId());
            detectionResponse.setCaseId(detection.getCaseId());
            detectionResponse.setModelId(detection.getModelId());
            detectionResponse.setModelName(modelService.getModelNameById(detection.getModelId()));
            detectionResponse.setStatus(detection.getStatus());
            detectionResponse.setCreatedAt(detection.getCreatedAt());
            detectionResponse.setCompletedAt(detection.getCompletedAt());
            detectionResponse.setImageResults(imageResults);
            
            Map<String, Object> result = new HashMap<>();
            result.put("analysis", overallAnalysis);
            result.put("totalDetections", allDetections.size());
            result.put("totalImages", imageResults.size());
            detectionResponse.setResult(result);

            String resultJson = objectMapper.writeValueAsString(detectionResponse);
            logger.info("[检测流程] 结果 JSON 长度：{} 字符", resultJson.length());
            logger.info("[检测流程] 结果 JSON 预览：{}", resultJson.substring(0, Math.min(500, resultJson.length())));

            detection.setStatus("completed");
            detection.setResult(resultJson);
            detection.setCompletedAt(LocalDateTime.now());
            detectionMapper.update(detection);
            logger.info("[检测流程] 检测状态更新为 completed, completedAt={}", detection.getCompletedAt());

            cacheService.cacheDetectionResult(detectionId, detectionResponse);
            cacheService.evictCaseDetailCache(caseId);

            logger.info("[状态更新] 病例ID: {} 状态从 '待检测' 变更为 '已检测'", caseId);
            caseService.updateCaseStatus(caseId, "已检测");
            logger.info("[检测流程] 病例状态更新为'已检测'");
            logger.info("[检测流程] ========== 检测任务完成 ==========");
        } catch (Exception e) {
            logger.error("[检测流程] ========== 检测任务异常 ==========");
            logger.error("[检测流程] 异常信息: {} - {}", e.getClass().getName(), e.getMessage(), e);
            
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "检测过程中发生未知错误";
            }
            
            Detection detection = detectionMapper.findById(detectionId).orElse(null);
            if (detection != null) {
                detection.setStatus("failed");
                try {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("error", errorMessage);
                    errorMap.put("message", "检测失败：" + errorMessage);
                    detection.setResult(objectMapper.writeValueAsString(errorMap));
                } catch (JsonProcessingException ex) {
                    logger.error("[检测流程] 错误信息 JSON 序列化失败：{}", ex.getMessage());
                    detection.setResult(null);
                }
                detection.setCompletedAt(LocalDateTime.now());
                detectionMapper.update(detection);
                logger.error("[检测流程] 检测状态更新为 failed, errorMessage={}", errorMessage);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private String generateAnalysis(List<DetectionResponse.DetectionItem> detections) {
        if (detections.isEmpty()) {
            return "AI模型未检测到任何病灶。建议结合临床症状进行进一步诊断。";
        }

        int count = detections.size();
        StringBuilder analysis = new StringBuilder();
        analysis.append("AI模型检测到").append(count).append("处病灶，");

        // 统计不同类型的病灶
        Map<String, Integer> labelCount = new HashMap<>();
        for (DetectionResponse.DetectionItem item : detections) {
            labelCount.put(item.getLabel(), labelCount.getOrDefault(item.getLabel(), 0) + 1);
        }

        // 添加病灶类型统计
        for (Map.Entry<String, Integer> entry : labelCount.entrySet()) {
            analysis.append(entry.getValue()).append("处").append(entry.getKey()).append("，");
        }

        // 添加置信度信息
        double avgConfidence = detections.stream()
                .mapToDouble(DetectionResponse.DetectionItem::getConfidence)
                .average()
                .orElse(0);
        analysis.append("平均置信度为").append(String.format("%.1f", avgConfidence * 100)).append("%。");

        analysis.append("建议结合临床症状进行进一步诊断。");

        return analysis.toString();
    }

    @Override
    public DetectionResponse getDetectionById(Long caseId, Long detectionId) {
        logger.info("[检测流程] ========== 3. 查询检测结果 ==========");
        logger.info("[检测流程] caseId={}, detectionId={}", caseId, detectionId);

        DetectionResponse cachedResult = cacheService.getDetectionResultCache(detectionId);
        if (cachedResult != null) {
            logger.info("[检测流程] 从缓存获取检测结果");
            return cachedResult;
        }

        Optional<Detection> optionalDetection = detectionMapper.findById(detectionId);
        if (!optionalDetection.isPresent()) {
            logger.error("[检测流程] 检测记录不存在！detectionId={}", detectionId);
            throw new RuntimeException("检测记录不存在");
        }

        Detection detection = optionalDetection.get();
        logger.info("[检测流程] 数据库记录: status={}, createdAt={}, completedAt={}", detection.getStatus(), detection.getCreatedAt(), detection.getCompletedAt());

        if (!detection.getCaseId().equals(caseId)) {
            logger.error("[检测流程] 检测记录不属于该病例！");
            throw new RuntimeException("检测记录不属于该病例");
        }

        DetectionResponse response = new DetectionResponse();
        response.setDetectionId(detection.getId());
        response.setCaseId(detection.getCaseId());
        response.setModelId(detection.getModelId());
        response.setModelName(modelService.getModelNameById(detection.getModelId()));
        response.setStatus(detection.getStatus());
        response.setCreatedAt(detection.getCreatedAt());
        response.setCompletedAt(detection.getCompletedAt());

        if (detection.getResult() != null && !detection.getResult().isEmpty()) {
            try {
                DetectionResponse storedResponse = objectMapper.readValue(detection.getResult(), DetectionResponse.class);
                response.setImageResults(storedResponse.getImageResults());
                response.setResult(storedResponse.getResult());
                logger.info("[检测流程] 解析结果 JSON 成功，imageResults 数量={}", storedResponse.getImageResults() != null ? storedResponse.getImageResults().size() : 0);
                logger.info("[检测流程] 整体分析结果：{}", storedResponse.getResult() != null ? storedResponse.getResult().get("analysis") : "null");
                if (storedResponse.getImageResults() != null) {
                    for (DetectionResponse.DetectionResult ir : storedResponse.getImageResults()) {
                        logger.info("[检测流程]   - imageId={}, imageName={}, detections={}个, analysis={}", ir.getImageId(), ir.getImageName(), ir.getDetections() != null ? ir.getDetections().size() : 0, ir.getAnalysis());
                    }
                }
                
                if ("completed".equals(detection.getStatus())) {
                    cacheService.cacheDetectionResult(detectionId, response);
                }
            } catch (JsonProcessingException e) {
                logger.error("[检测流程] 解析结果JSON失败: {}", e.getMessage());
            }
        } else {
            logger.info("[检测流程] 结果JSON为空（可能检测还在进行中或失败）");
        }

        logger.info("[检测流程] 返回检测结果给前端, status={}", response.getStatus());
        return response;
    }

    @Override
    public void writeDetectionResultToResponse(DetectionResponse detectionResponse, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        if (!"completed".equals(detectionResponse.getStatus())) {
            throw new RuntimeException("检测尚未完成，无法导出结果");
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try (java.io.PrintWriter writer = response.getWriter()) {
            objectMapper.writeValue(writer, detectionResponse);
            writer.flush();
        }
    }

    @Override
    public void exportDetectionResultAsZip(Long caseId, Long detectionId, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        logger.info("[导出] 开始导出检测结果ZIP: caseId={}, detectionId={}", caseId, detectionId);
        
        DetectionResponse detectionResponse = getDetectionById(caseId, detectionId);
        if (!"completed".equals(detectionResponse.getStatus())) {
            throw new RuntimeException("检测尚未完成，无法导出结果");
        }

        List<ImageResponse> images = imageService.getImagesByCaseId(caseId);
        
        response.setContentType("application/zip");
        response.setCharacterEncoding("UTF-8");
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String zipFileName = "detection_result_" + detectionId + "_" + timestamp + ".zip";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
        
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            Map<String, Object> exportData = buildExportJson(detectionResponse, caseId);
            
            byte[] jsonBytes = objectMapper.writeValueAsBytes(exportData);
            zos.putNextEntry(new ZipEntry("detection_result.json"));
            zos.write(jsonBytes);
            zos.closeEntry();
            
            if (detectionResponse.getImageResults() != null) {
                for (DetectionResponse.DetectionResult imageResult : detectionResponse.getImageResults()) {
                    if (imageResult.getDetections() != null && !imageResult.getDetections().isEmpty()) {
                        ImageResponse matchedImage = images.stream()
                                .filter(img -> img.getImageId().equals(imageResult.getImageId()))
                                .findFirst()
                                .orElse(null);
                        
                        if (matchedImage != null) {
                            try {
                                File imageFile = fileUtils.getFile(matchedImage.getFilePath());
                                if (imageFile.exists()) {
                                    BufferedImage originalImage = ImageIO.read(imageFile);
                                    if (originalImage != null) {
                                        BufferedImage annotatedImage = drawAnnotations(originalImage, imageResult.getDetections());
                                        
                                        String imageFileName = "annotated_" + (matchedImage.getFileName() != null ? matchedImage.getFileName() : "image_" + imageResult.getImageId() + ".png");
                                        if (!imageFileName.toLowerCase().endsWith(".png")) {
                                            imageFileName = imageFileName.substring(0, imageFileName.lastIndexOf('.')) + ".png";
                                        }
                                        
                                        zos.putNextEntry(new ZipEntry(imageFileName));
                                        ImageIO.write(annotatedImage, "PNG", zos);
                                        zos.closeEntry();
                                        
                                        logger.info("[导出] 标注图已生成: {}", imageFileName);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("[导出] 生成标注图失败: imageId={}", imageResult.getImageId(), e);
                            }
                        }
                    }
                }
            }
            
            zos.finish();
        }
        
        logger.info("[导出] ZIP导出完成");
    }

    private Map<String, Object> buildExportJson(DetectionResponse detectionResponse, Long caseId) {
        Map<String, Object> exportData = new HashMap<>();
        
        exportData.put("detectionId", detectionResponse.getDetectionId());
        exportData.put("caseId", caseId);
        exportData.put("modelId", detectionResponse.getModelId());
        exportData.put("modelName", detectionResponse.getModelName());
        exportData.put("status", detectionResponse.getStatus());
        exportData.put("createdAt", detectionResponse.getCreatedAt());
        exportData.put("completedAt", detectionResponse.getCompletedAt());
        
        if (detectionResponse.getResult() != null) {
            exportData.put("analysis", detectionResponse.getResult().get("analysis"));
            exportData.put("totalDetections", detectionResponse.getResult().get("totalDetections"));
            exportData.put("totalImages", detectionResponse.getResult().get("totalImages"));
        }
        
        List<Map<String, Object>> imageResults = new ArrayList<>();
        if (detectionResponse.getImageResults() != null) {
            for (DetectionResponse.DetectionResult imageResult : detectionResponse.getImageResults()) {
                Map<String, Object> imageMap = new HashMap<>();
                imageMap.put("imageId", imageResult.getImageId());
                imageMap.put("imageName", imageResult.getImageName());
                imageMap.put("analysis", imageResult.getAnalysis());
                
                List<Map<String, Object>> detections = new ArrayList<>();
                if (imageResult.getDetections() != null) {
                    for (DetectionResponse.DetectionItem det : imageResult.getDetections()) {
                        Map<String, Object> detMap = new HashMap<>();
                        detMap.put("label", det.getLabel());
                        detMap.put("confidence", det.getConfidence());
                        detMap.put("x", det.getX());
                        detMap.put("y", det.getY());
                        detMap.put("width", det.getWidth());
                        detMap.put("height", det.getHeight());
                        detections.add(detMap);
                    }
                }
                imageMap.put("detections", detections);
                imageResults.add(imageMap);
            }
        }
        exportData.put("imageResults", imageResults);
        
        return exportData;
    }

    private BufferedImage drawAnnotations(BufferedImage image, List<DetectionResponse.DetectionItem> detections) {
        BufferedImage annotatedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = annotatedImage.createGraphics();
        
        g2d.drawImage(image, 0, 0, null);
        g2d.setStroke(new BasicStroke(3));
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        
        Color[] colors = {
            Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, 
            Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.PINK
        };
        int colorIndex = 0;
        
        for (DetectionResponse.DetectionItem det : detections) {
            Color color = colors[colorIndex % colors.length];
            colorIndex++;
            
            g2d.setColor(color);
            int x = (int) Math.round(det.getX());
            int y = (int) Math.round(det.getY());
            int w = (int) Math.round(det.getWidth());
            int h = (int) Math.round(det.getHeight());
            
            g2d.drawRect(x, y, w, h);
            
            String label = det.getLabel() + " (" + String.format("%.1f", det.getConfidence() * 100) + "%)";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(label);
            int textHeight = fm.getHeight();
            
            int labelY = Math.max(y - 2, textHeight);
            
            g2d.setColor(color);
            g2d.fillRect(x, labelY - textHeight, textWidth + 4, textHeight);
            
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, x + 2, labelY - 2);
        }
        
        g2d.dispose();
        return annotatedImage;
    }

    @Override //0412新增方法，用于检测结果的保存后展示
    public List<DetectionResponse> getDetectionsByCaseId(Long caseId) {
        List<Detection> detections = detectionMapper.findByCaseId(caseId);
        List<DetectionResponse> responses = new ArrayList<>();
        for (Detection detection : detections) {
            DetectionResponse response = new DetectionResponse();
            response.setDetectionId(detection.getId());
            response.setCaseId(detection.getCaseId());
            response.setModelId(detection.getModelId());
            response.setModelName(modelService.getModelNameById(detection.getModelId()));
            response.setStatus(detection.getStatus());
            response.setCreatedAt(detection.getCreatedAt());
            response.setCompletedAt(detection.getCompletedAt());

            if (detection.getResult() != null && !detection.getResult().isEmpty()) {
                try {
                    DetectionResponse storedResponse = objectMapper.readValue(detection.getResult(), DetectionResponse.class);
                    response.setImageResults(storedResponse.getImageResults());
                    // 设置整体的result对象，包含analysis
                    response.setResult(storedResponse.getResult());
                } catch (JsonProcessingException e) {
                    System.err.println("[检测流程] 解析结果JSON失败: " + e.getMessage());
                }
            }
            responses.add(response);
        }
        return responses;
    }

    @Override
    public void submitFeedback(Long caseId, Long detectionId, FeedbackRequest request, String username) {
        Detection detection = detectionMapper.findById(detectionId).orElseThrow(() -> new RuntimeException("检测记录不存在"));
        if (!detection.getCaseId().equals(caseId)) {
            throw new RuntimeException("检测记录不属于该病例");
        }

        List<Feedback> existingFeedbacks = feedbackMapper.findByDetectionId(detectionId);
        if (!existingFeedbacks.isEmpty()) {
            throw new RuntimeException("已经提交过反馈");
        }

        Feedback feedback = new Feedback();
        feedback.setDetectionId(detectionId);
        feedback.setEvaluation(request.getEvaluation());
        feedback.setFeedback(request.getFeedback() != null ? request.getFeedback() : "");
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setCreatedBy(username);

        feedbackMapper.insert(feedback);

        modelService.recalculateModelAccuracy(detection.getModelId());
    }

    @Override
    public Map<String, Object> getFeedbackList(int page, int pageSize) {
        int offset = (page - 1) * pageSize;

        long total = feedbackMapper.countAll();

        List<Map<String, Object>> rawData = feedbackMapper.findAllWithDetails(offset, pageSize);

        List<FeedbackListItem> feedbackList = new ArrayList<>();

        for (Map<String, Object> row : rawData) {
            FeedbackListItem item = new FeedbackListItem();
            item.setId((Long) row.get("id"));
            item.setCaseNo("CASE-" + row.get("case_id"));
            item.setPatientName((String) row.get("patient_name"));

            // ====== 修复开始 ======
            // detect_time 转换
            Object detectTimeObj = row.get("detect_time");
            if (detectTimeObj instanceof java.sql.Timestamp) {
                item.setDetectTime(((java.sql.Timestamp) detectTimeObj).toLocalDateTime());
            }

            // created_at 转换
            Object createdAtObj = row.get("created_at");
            if (createdAtObj instanceof java.sql.Timestamp) {
                item.setFeedbackTime(((java.sql.Timestamp) createdAtObj).toLocalDateTime());
            }
            // ====== 修复结束 ======

            item.setEvaluation((String) row.get("evaluation"));
            item.setOperator((String) row.get("created_by"));
            item.setFeedback((String) row.get("feedback"));
            feedbackList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (total + pageSize - 1) / pageSize);
        result.put("data", feedbackList);

        return result;
    }
}