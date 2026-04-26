package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.annotation.AuditLog;
import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.dto.detection.DetectionRequest;
import com.xycy.chestimaging.dto.detection.DetectionResponse;
import com.xycy.chestimaging.dto.detection.FeedbackRequest;
import com.xycy.chestimaging.exception.AccessDeniedException;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.service.DetectionService;
import com.xycy.chestimaging.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/cases/{caseId}/detection")
public class DetectionController {
    @Autowired
    private DetectionService detectionService;
    @Autowired
    private UserService userService;

    private void checkDoctorRole() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user.getRole() != User.Role.doctor) {
            throw new AccessDeniedException("权限不足，只有医生可以操作");
        }
    }

    private void checkDoctorResearcherRole() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user.getRole() != User.Role.doctor && user.getRole() != User.Role.researcher) {
            throw new AccessDeniedException("权限不足，只有医生和科研人员可以操作");
        }
    }

    @PostMapping
    @AuditLog(operationType = "CREATE_DETECTION", operationContent = "创建检测任务")
    public Response<?> createDetection(@PathVariable Long caseId, @RequestBody DetectionRequest request) {
        try {
            checkDoctorRole();
            
            // 参数校验
            if (request == null) {
                return Response.error(400, "请求参数不能为空");
            }
            if (request.getModelId() == null) {
                return Response.error(400, "模型 ID 不能为空");
            }
            
            return Response.success("检测任务已启动", detectionService.createDetection(caseId, request));
        } catch (IllegalArgumentException e) {
            System.err.println("[检测控制器] 参数错误：" + e.getMessage());
            e.printStackTrace();
            return Response.error(400, "参数错误", e.getMessage());
        } catch (Exception e) {
            System.err.println("[检测控制器] 创建检测任务失败：" + e.getMessage());
            e.printStackTrace();
            return Response.error(500, "检测任务创建失败", e.getMessage());
        }
    }

    @GetMapping("/{detectionId}")
    @AuditLog(operationType = "QUERY_DETECTION", operationContent = "查询检测结果")
    public Response<?> getDetectionById(@PathVariable Long caseId, @PathVariable Long detectionId) {
        try {
            checkDoctorResearcherRole();
            return Response.success("查询成功", detectionService.getDetectionById(caseId, detectionId));
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(500, "查询失败", e.getMessage());
        }
    }

    @GetMapping("/{detectionId}/export")
    @AuditLog(operationType = "EXPORT_DETECTION", operationContent = "导出检测结果")
    public void exportDetectionResult(@PathVariable Long caseId, @PathVariable Long detectionId, 
                                      @RequestParam(defaultValue = "json") String format,
                                      HttpServletResponse response) {
        try {
            checkDoctorResearcherRole();
            
            if ("zip".equalsIgnoreCase(format)) {
                detectionService.exportDetectionResultAsZip(caseId, detectionId, response);
            } else {
                exportAsJson(caseId, detectionId, response);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void exportAsJson(Long caseId, Long detectionId, HttpServletResponse response) throws IOException {
        DetectionResponse detectionResponse = detectionService.getDetectionById(caseId, detectionId);
        
        if (!"completed".equals(detectionResponse.getStatus())) {
            throw new RuntimeException("检测尚未完成，无法导出结果");
        }
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String fileName = "detection_result_" + detectionId + "_" + System.currentTimeMillis() + ".json";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        
        detectionService.writeDetectionResultToResponse(detectionResponse, response);
    }

    @PostMapping("/{detectionId}/feedback")
    @AuditLog(operationType = "FEEDBACK_DETECTION", operationContent = "提交检测反馈")
    public Response<?> submitFeedback(@PathVariable Long caseId, @PathVariable Long detectionId, @RequestBody FeedbackRequest request) {
        try {
            checkDoctorResearcherRole();
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            detectionService.submitFeedback(caseId, detectionId, request, username);
            return Response.success("反馈提交成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(500, "反馈提交失败", e.getMessage());
        }
    }

}