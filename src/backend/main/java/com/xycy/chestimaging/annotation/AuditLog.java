package com.xycy.chestimaging.annotation;

import java.lang.annotation.*;

// 审计日志注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    
    // 操作类型
    String operationType() default "OPERATION";
    
    // 操作内容描述
    String operationContent() default "";
}