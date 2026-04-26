package com.xycy.chestimaging.service;

import com.xycy.chestimaging.dto.detection.DetectionRequest;
import com.xycy.chestimaging.dto.detection.DetectionResponse;
import com.xycy.chestimaging.dto.detection.FeedbackRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DetectionService {
    DetectionResponse createDetection(Long caseId, DetectionRequest request);
    DetectionResponse getDetectionById(Long caseId, Long detectionId);
    void writeDetectionResultToResponse(DetectionResponse detectionResponse, HttpServletResponse response) throws IOException;
    void exportDetectionResultAsZip(Long caseId, Long detectionId, HttpServletResponse response) throws IOException;
    void submitFeedback(Long caseId, Long detectionId, FeedbackRequest request, String username);
    List<DetectionResponse> getDetectionsByCaseId(Long caseId);

    Map<String, Object> getFeedbackList(int page, int pageSize);
}
