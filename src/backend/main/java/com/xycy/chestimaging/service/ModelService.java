package com.xycy.chestimaging.service;

import com.xycy.chestimaging.dto.model.ModelResponse;
import com.xycy.chestimaging.model.Model;

import java.util.List;

public interface ModelService {
    List<ModelResponse> getModels();
    ModelResponse activateModel(Long modelId);
    String getModelNameById(Long modelId);
    void recalculateModelAccuracy(Long modelId);
}
