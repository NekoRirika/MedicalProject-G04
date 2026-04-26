package com.xycy.chestimaging.enums;

/**
 * 审计日志操作类型枚举
 */
public enum OperationType {
    
    // 认证相关
    LOGIN("LOGIN", "用户登录"),
    LOGOUT("LOGOUT", "用户登出"),
    
    // 病例管理
    CREATE_CASE("CREATE_CASE", "创建病例"),
    QUERY_CASE("QUERY_CASE", "查询病例"),
    UPDATE_CASE("UPDATE_CASE", "更新病例"),
    DELETE_CASE("DELETE_CASE", "删除病例"),
    
    // 影像管理
    UPLOAD_IMAGE("UPLOAD_IMAGE", "上传影像"),
    QUERY_IMAGE("QUERY_IMAGE", "查询影像"),
    DELETE_IMAGE("DELETE_IMAGE", "删除影像"),
    
    // 检测管理
    CREATE_DETECTION("CREATE_DETECTION", "创建检测任务"),
    QUERY_DETECTION("QUERY_DETECTION", "查询检测结果"),
    EXPORT_DETECTION("EXPORT_DETECTION", "导出检测结果"),
    FEEDBACK_DETECTION("FEEDBACK_DETECTION", "提交检测反馈"),
    
    // 用户管理
    CREATE_USER("CREATE_USER", "创建用户"),
    QUERY_USER("QUERY_USER", "查询用户"),
    UPDATE_USER("UPDATE_USER", "更新用户"),
    DELETE_USER("DELETE_USER", "删除用户"),
    RESET_PASSWORD("RESET_PASSWORD", "重置密码"),
    UPDATE_STATUS("UPDATE_STATUS", "更新状态"),
    
    // 审计日志
    QUERY_AUDIT_LOG("QUERY_AUDIT_LOG", "查询审计日志"),
    
    // 通用操作
    CREATE("CREATE", "创建"),
    QUERY("QUERY", "查询"),
    UPDATE("UPDATE", "更新"),
    DELETE("DELETE", "删除"),
    UPLOAD("UPLOAD", "上传"),
    EXPORT("EXPORT", "导出"),
    DOWNLOAD("DOWNLOAD", "下载"),
    IMPORT("IMPORT", "导入"),
    OTHER("OTHER", "其他");
    
    private final String code;
    private final String description;
    
    OperationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据 code 获取枚举
     */
    public static OperationType fromCode(String code) {
        for (OperationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
    
    /**
     * 判断是否为有效的操作类型
     */
    public static boolean isValid(String code) {
        for (OperationType type : values()) {
            if (type.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}