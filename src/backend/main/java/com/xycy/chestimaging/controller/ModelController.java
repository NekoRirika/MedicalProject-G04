package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.exception.AccessDeniedException;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.service.ModelService;
import com.xycy.chestimaging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/models")
public class ModelController {
    @Autowired
    private ModelService modelService;
    @Autowired
    private UserService userService;

    private void checkAdminResearcherRole() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user.getRole() != User.Role.admin && user.getRole() != User.Role.researcher) {
            throw new AccessDeniedException("权限不足，只有管理员和科研人员可以操作");
        }
    }

    @GetMapping
    public Response<?> getModels() {
        return Response.success("查询成功", modelService.getModels());
    }

    @PostMapping("/{modelId}/activate")
    public Response<?> activateModel(@PathVariable Long modelId) {
        checkAdminResearcherRole();
        return Response.success("模型切换成功", modelService.activateModel(modelId));
    }

    @PostMapping("/{modelId}/recalculate-accuracy")
    public Response<?> recalculateAccuracy(@PathVariable Long modelId) {
        checkAdminResearcherRole();
        modelService.recalculateModelAccuracy(modelId);
        return Response.success("准确率重新计算成功");
    }
}