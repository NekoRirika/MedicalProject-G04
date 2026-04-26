package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.exception.AccessDeniedException;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.service.DetectionService;
import com.xycy.chestimaging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {
    @Autowired
    private DetectionService detectionService;
    @Autowired
    private UserService userService;

    private void checkDoctorResearcherRole() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user.getRole() != User.Role.doctor && user.getRole() != User.Role.researcher) {
            throw new AccessDeniedException("权限不足，只有医生和科研人员可以操作");
        }
    }

    @GetMapping
    public Response<Map<String,Object>> getFeedbacks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize){

        try {
            checkDoctorResearcherRole();
            Map<String,Object> feedbacks = detectionService.getFeedbackList(page, pageSize);
            return Response.success("查询成功", feedbacks);
        } catch (AccessDeniedException e) {
            return Response.error(403, "权限不足", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(500, "查询失败", e.getMessage());
        }
    }
}