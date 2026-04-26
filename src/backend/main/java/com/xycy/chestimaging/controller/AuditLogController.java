package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.exception.AccessDeniedException;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.service.AuditLogService;
import com.xycy.chestimaging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {
    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private UserService userService;

    private void checkAdminRole() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user.getRole() != User.Role.admin) {
            throw new AccessDeniedException("权限不足，只有管理员可以操作");
        }
    }

    @GetMapping
    public Response<?> getAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int page_size,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String operation_type,
            @RequestParam(required = false) String start_time,
            @RequestParam(required = false) String end_time) {
        checkAdminRole();
        return Response.success("查询成功", auditLogService.getAuditLogs(page, page_size, operator, operation_type, start_time, end_time));
    }

    @GetMapping("/{id}")
    public Response<?> getAuditLogById(@PathVariable Long id) {
        checkAdminRole();
        return Response.success("查询成功", auditLogService.getAuditLogById(id));
    }
}