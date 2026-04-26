package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.annotation.AuditLog;
import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.dto.user.UserCreateRequest;
import com.xycy.chestimaging.dto.user.UserStatusUpdateRequest;
import com.xycy.chestimaging.dto.user.UserUpdateRequest;
import com.xycy.chestimaging.exception.AccessDeniedException;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
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
    @AuditLog(operationType = "QUERY_USER", operationContent = "查询用户列表")
    public Response<?> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int page_size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String department) {
        checkAdminRole();
        return Response.success("查询成功", userService.getUsers(page, page_size, username, department));
    }

    @PostMapping
    @AuditLog(operationType = "CREATE_USER", operationContent = "创建用户")
    public Response<?> createUser(@RequestBody UserCreateRequest request) {
        checkAdminRole();
        return Response.success("创建成功", userService.createUser(request));
    }

    @PutMapping("/{id}")
    @AuditLog(operationType = "UPDATE_USER", operationContent = "更新用户信息")
    public Response<?> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        checkAdminRole();
        return Response.success("编辑成功", userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditLog(operationType = "DELETE_USER", operationContent = "删除用户")
    public Response<?> deleteUser(@PathVariable Long id) {
        checkAdminRole();
        userService.deleteUser(id);
        return Response.success("删除成功");
    }

    @PostMapping("/{id}/reset-password")
    @AuditLog(operationType = "RESET_PASSWORD", operationContent = "重置用户密码")
    public Response<?> resetPassword(@PathVariable Long id) {
        checkAdminRole();
        String newPassword = userService.resetPassword(id);
        return Response.success("密码重置成功", newPassword);
    }

    @PutMapping("/{id}/status")
    @AuditLog(operationType = "UPDATE_STATUS", operationContent = "更新用户状态")
    public Response<?> updateUserStatus(@PathVariable Long id, @RequestBody UserStatusUpdateRequest request) {
        checkAdminRole();
        return Response.success("状态更新成功", userService.updateUserStatus(id, request.getStatus()));
    }
}