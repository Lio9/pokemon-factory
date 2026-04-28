package com.lio9.common.response;



/**
 * ResponseCode 文件说明
 * 所属模块：common 公共模块。
 * 文件类型：公共响应封装文件。
 * 核心职责：负责统一接口返回结构、响应码或导入结果表达方式。
 * 阅读建议：建议结合控制器层返回格式一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

/**
 * 响应状态码常量类
 * 提供统一的HTTP状态码和业务状态码定义
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-02-12
 */
public class ResponseCode {
    
    // ==================== HTTP状态码 ====================
    
    /** 成功 */
    public static final int SUCCESS = 200;
    
    /** 创建成功 */
    public static final int CREATED = 201;
    
    /** 请求参数错误 */
    public static final int BAD_REQUEST = 400;
    
    /** 未授权 */
    public static final int UNAUTHORIZED = 401;
    
    /** 禁止访问 */
    public static final int FORBIDDEN = 403;
    
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    
    /** 方法不允许 */
    public static final int METHOD_NOT_ALLOWED = 405;
    
    /** 服务器内部错误 */
    public static final int INTERNAL_SERVER_ERROR = 500;
    
    /** 服务不可用 */
    public static final int SERVICE_UNAVAILABLE = 503;
    
    // ==================== 业务状态码 ====================
    
    /** 操作成功 */
    public static final int OPERATION_SUCCESS = 1000;
    
    /** 操作失败 */
    public static final int OPERATION_FAILED = 1001;
    
    /** 数据已存在 */
    public static final int DATA_ALREADY_EXISTS = 1002;
    
    /** 数据不存在 */
    public static final int DATA_NOT_FOUND = 1003;
    
    /** 参数验证失败 */
    public static final int VALIDATION_FAILED = 1004;
    
    /** 权限不足 */
    public static final int INSUFFICIENT_PERMISSION = 1005;
    
    /** 资源锁定 */
    public static final int RESOURCE_LOCKED = 1006;
    
    /** 超时 */
    public static final int TIMEOUT = 1007;
    
    /** 导入成功 */
    public static final int IMPORT_SUCCESS = 2000;
    
    /** 导入失败 */
    public static final int IMPORT_FAILED = 2001;
    
    /** 导入中 */
    public static final int IMPORTING = 2002;
    
    /** 导入完成 */
    public static final int IMPORT_COMPLETED = 2003;
    
    /** 导入取消 */
    public static final int IMPORT_CANCELLED = 2004;
    
    // ==================== 错误消息 ====================
    
    /** 成功消息 */
    public static final String MSG_SUCCESS = "操作成功";
    
    /** 失败消息 */
    public static final String MSG_FAILED = "操作失败";
    
    /** 数据已存在消息 */
    public static final String MSG_DATA_ALREADY_EXISTS = "数据已存在";
    
    /** 数据不存在消息 */
    public static final String MSG_DATA_NOT_FOUND = "数据不存在";
    
    /** 参数验证失败消息 */
    public static final String MSG_VALIDATION_FAILED = "参数验证失败";
    
    /** 权限不足消息 */
    public static final String MSG_INSUFFICIENT_PERMISSION = "权限不足";
    
    /** 超时消息 */
    public static final String MSG_TIMEOUT = "操作超时";
    
    /** 导入成功消息 */
    public static final String MSG_IMPORT_SUCCESS = "导入成功";
    
    /** 导入失败消息 */
    public static final String MSG_IMPORT_FAILED = "导入失败";
    
    /** 导入中消息 */
    public static final String MSG_IMPORTING = "导入中...";
    
    /** 导入完成消息 */
    public static final String MSG_IMPORT_COMPLETED = "导入完成";
    
    /** 导入取消消息 */
    public static final String MSG_IMPORT_CANCELLED = "导入已取消";
    
    // ==================== 私有构造函数 ====================
    
    /** 防止实例化 */
    private ResponseCode() {
        throw new AssertionError("常量类不能被实例化");
    }
}