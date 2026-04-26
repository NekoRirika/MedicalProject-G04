package com.xycy.chestimaging.service.impl;

import com.xycy.chestimaging.dto.model.ModelResponse;
import com.xycy.chestimaging.mapper.DetectionMapper;
import com.xycy.chestimaging.mapper.FeedbackMapper;
import com.xycy.chestimaging.mapper.ModelMapper;
import com.xycy.chestimaging.model.Detection;
import com.xycy.chestimaging.model.Feedback;
import com.xycy.chestimaging.model.Model;
import com.xycy.chestimaging.service.CacheService;
import com.xycy.chestimaging.service.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ModelServiceImpl implements ModelService {
    private static final Logger logger = LoggerFactory.getLogger(ModelServiceImpl.class);
    
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private DetectionMapper detectionMapper;
    @Autowired
    private FeedbackMapper feedbackMapper;
    @Autowired
    private CacheService cacheService;

    @Override
    public List<ModelResponse> getModels() {
        List<Model> models = modelMapper.findAll();
        return models.stream()
                .map(ModelResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public ModelResponse activateModel(Long modelId) {
        logger.info("[模型激活] 开始激活模型，modelId={}", modelId);
        
        Optional<Model> optionalModel = modelMapper.findById(modelId);
        if (!optionalModel.isPresent()) {
            logger.error("[模型激活] 模型不存在，modelId={}", modelId);
            throw new RuntimeException("模型不存在");
        }
        logger.info("[模型激活] 找到模型，name={}, version={}", optionalModel.get().getName(), optionalModel.get().getVersion());

        logger.info("[模型激活] 开始停用所有模型");
        modelMapper.deactivateAll();
        logger.info("[模型激活] 所有模型已停用");

        Model model = optionalModel.get();
        model.setStatus("active");
        model.setActivatedAt(LocalDateTime.now());
        logger.info("[模型激活] 开始更新模型状态为 active, modelId={}, status={}, activatedAt={}", model.getId(), model.getStatus(), model.getActivatedAt());
        try {
            int rows = modelMapper.update(model);
            logger.info("[模型激活] 模型状态更新成功, 影响行数={}", rows);
        } catch (Exception e) {
            logger.error("[模型激活] 模型状态更新失败, modelId={}", modelId, e);
            throw new RuntimeException("模型状态更新失败: " + e.getMessage(), e);
        }
        
        cacheService.evictModelInfo(modelId);
        logger.info("[模型激活] 模型已激活，缓存已删除: {}", modelId);

        return new ModelResponse(model);
    }

    @Override
    public String getModelNameById(Long modelId) {
        Model cachedModel = cacheService.getModelInfo(modelId);
        if (cachedModel != null) {
            logger.info("[模型查询] 从Redis缓存获取模型信息: {}", modelId);
            return cachedModel.getName() + " " + cachedModel.getVersion();
        }
        
        logger.info("[模型查询] 缓存未命中，从数据库查询: {}", modelId);
        Optional<Model> optionalModel = modelMapper.findById(modelId);
        if (!optionalModel.isPresent()) {
            return "未知模型";
        }
        Model model = optionalModel.get();
        cacheService.cacheModelInfo(modelId, model);
        return model.getName() + " " + model.getVersion();
    }

    @Override
    public void recalculateModelAccuracy(Long modelId) {
        Optional<Model> optionalModel = modelMapper.findById(modelId);
        if (!optionalModel.isPresent()) {
            throw new RuntimeException("模型不存在");
        }

        List<Detection> detections = detectionMapper.findByModelId(modelId);
        if (detections == null || detections.isEmpty()) {
            logger.info("[准确率计算] 模型 {} 暂无检测记录，保持原有准确率", modelId);
            return;
        }

        int totalFeedbacks = 0;
        int positiveFeedbacks = 0;

        for (Detection detection : detections) {
            List<Feedback> feedbacks = feedbackMapper.findByDetectionId(detection.getId());
            if (feedbacks != null) {
                for (Feedback feedback : feedbacks) {
                    totalFeedbacks++;
                    if ("准确".equals(feedback.getEvaluation())) {
                        positiveFeedbacks++;
                    }
                }
            }
        }

        if (totalFeedbacks == 0) {
            logger.info("[准确率计算] 模型 {} 暂无反馈记录，保持原有准确率", modelId);
            return;
        }

        double newAccuracy = (double) positiveFeedbacks / totalFeedbacks;
        Model model = optionalModel.get();
        double oldAccuracy = model.getAccuracy();
        model.setAccuracy(newAccuracy);
        modelMapper.updateAccuracy(modelId, newAccuracy);
        
        cacheService.evictModelInfo(modelId);
        logger.info("[准确率计算] 模型 {} 准确率已更新: {} -> {} (基于 {} 条反馈)", 
                modelId, oldAccuracy, newAccuracy, totalFeedbacks);
    }
}
