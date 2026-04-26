package com.xycy.chestimaging.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xycy.chestimaging.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class AuditLogAspect {   

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ObjectMapper objectMapper;

    // 记录操作成功
    @AfterReturning(pointcut = "@annotation(com.xycy.chestimaging.annotation.AuditLog)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        recordAuditLog(joinPoint, "success", null);
    }

    // 记录操作失败
    @AfterThrowing(pointcut = "@annotation(com.xycy.chestimaging.annotation.AuditLog)", throwing = "exception")
    public void afterThrowing(JoinPoint joinPoint, Exception exception) {
        recordAuditLog(joinPoint, "failed", exception.getMessage());
    }

    // 记录审计日志
    private void recordAuditLog(JoinPoint joinPoint, String status, String errorMessage) {
        try {
            // 获取方法签名
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 获取审计日志注解
            com.xycy.chestimaging.annotation.AuditLog auditLogAnnotation = method.getAnnotation(com.xycy.chestimaging.annotation.AuditLog.class);

            // 获取操作类型和内容
            String operationType = auditLogAnnotation.operationType();
            String operationContent = auditLogAnnotation.operationContent();

            // 获取当前用户
            String operator = getCurrentUser();

            // 获取 IP 地址
            String ipAddress = getClientIp();

            // 构建详细信息
            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("className", joinPoint.getTarget().getClass().getSimpleName());
            detailsMap.put("methodName", method.getName());
            detailsMap.put("arguments", parseArguments(joinPoint.getArgs()));
            
            if ("failed".equals(status)) {
                detailsMap.put("error", errorMessage);
            }

            String details = objectMapper.writeValueAsString(detailsMap);

            // 创建审计日志
            auditLogService.createAuditLog(
                operator,
                operationType,
                operationContent,
                ipAddress,
                status,
                details
            );

        } catch (Exception e) {
            // 记录日志失败不影响原业务逻辑
            System.err.println("记录审计日志失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // 获取当前登录用户
    private String getCurrentUser() {
        try {
            // 从 Spring Security 上下文中获取认证信息
            org.springframework.security.core.context.SecurityContext context = 
                org.springframework.security.core.context.SecurityContextHolder.getContext();
            
            if (context != null && context.getAuthentication() != null) {
                Object principal = context.getAuthentication().getPrincipal();
                
                // 如果是 UserDetails 类型，获取 username
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                }
                
                // 如果是字符串，直接返回
                if (principal instanceof String) {
                    String principalStr = (String) principal;
                    if (!principalStr.isEmpty() && !"anonymousUser".equals(principalStr)) {
                        return principalStr;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常，使用备用方案
            System.err.println("从 SecurityContext 获取用户失败：" + e.getMessage());
        }
        
        // 备用方案：从请求头中获取用户信息（如果有）
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userHeader = request.getHeader("X-User-Name");
                if (userHeader != null && !userHeader.isEmpty()) {
                    return userHeader;
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        
        return "anonymous";
    }

    // 获取客户端 IP 地址
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                // 处理 IPv6 本地回环地址
                if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                    ip = "127.0.0.1";
                }
                return ip;
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return "unknown";
    }

    // 解析方法参数
    private Map<String, Object> parseArguments(Object[] args) {
        Map<String, Object> argMap = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                Object value = serializeArgument(args[i]);
                if (value != null) {
                    argMap.put("arg" + i, value);
                }
            }
        }
        return argMap;
    }

    // 安全地序列化参数
    private Object serializeArgument(Object arg) {
        try {
            // 跳过无法序列化的类型
            if (arg instanceof jakarta.servlet.http.HttpServletRequest ||
                arg instanceof jakarta.servlet.http.HttpServletResponse ||
                arg instanceof org.springframework.web.multipart.MultipartFile ||
                arg instanceof java.io.InputStream ||
                arg instanceof java.io.OutputStream) {
                return "[unsupported type: " + arg.getClass().getSimpleName() + "]";
            }

            // 尝试序列化，如果失败则返回 toString
            try {
                objectMapper.writeValueAsString(arg);
                return arg;
            } catch (Exception e) {
                // 如果序列化失败，尝试返回简单信息
                if (arg.getClass().isPrimitive() || 
                    arg instanceof String || 
                    arg instanceof Number || 
                    arg instanceof Boolean) {
                    return arg;
                }
                return "[object: " + arg.getClass().getSimpleName() + "]";
            }
        } catch (Exception e) {
            return "[serialization failed]";
        }
    }
}