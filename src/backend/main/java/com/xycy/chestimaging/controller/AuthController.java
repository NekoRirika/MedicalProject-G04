package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.annotation.AuditLog;
import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.dto.user.LoginRequest;
import com.xycy.chestimaging.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @AuditLog(operationType = "LOGIN", operationContent = "用户登录")
    public Response<?> login(@RequestBody LoginRequest request) {
        try {
            return Response.success("登录成功", authService.login(request));
        } catch (RuntimeException e) {
            return Response.error(401, "登录失败", e.getMessage());
        }
    }

    @PostMapping("/logout")
    @AuditLog(operationType = "LOGOUT", operationContent = "用户登出")
    public Response<?> logout(jakarta.servlet.http.HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String authorization = request.getHeader("Authorization");
        String token = authorization.substring(7);
        authService.logout(username, token);
        return Response.success("登出成功");
    }
}