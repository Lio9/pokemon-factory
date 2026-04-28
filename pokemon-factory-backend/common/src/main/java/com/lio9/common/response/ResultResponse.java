package com.lio9.common.response;



/**
 * ResultResponse 文件说明
 * 所属模块：common 公共模块。
 * 文件类型：公共响应封装文件。
 * 核心职责：负责统一接口返回结构、响应码或导入结果表达方式。
 * 阅读建议：建议结合控制器层返回格式一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import java.util.HashMap;
import java.util.Map;

/**
 * 通用响应构建器
 * 提供统一的成功和失败响应构建方法
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-02-12
 */
public class ResultResponse {
    
    /**
     * 构建通用成功响应
     * 
     * @param code 状态码
     * @param message 消息内容
     * @param data 数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildSuccessResponse(int code, String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);
        return result;
    }
    
    /**
     * 构建自定义失败响应
     * 
     * @param code 状态码
     * @param message 消息
     * @param error 错误信息
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildCustomErrorResponse(int code, String message, String error) {
        return buildCustomErrorResponse(code, message, error, null);
    }

    /**
     * 构建自定义失败响应，并在需要时附带结构化上下文数据。
     * 
     * @param code 状态码
     * @param message 消息
     * @param error 错误信息
     * @param data 额外上下文数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildCustomErrorResponse(int code, String message, String error, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("error", error);
        if (data != null) {
            result.put("data", data);
        }
        return result;
    }
    
    /**
     * 构建成功响应（默认消息）
     * 
     * @param data 数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildSuccess(Object data) {
        return buildSuccessResponse(ResponseCode.SUCCESS, ResponseCode.MSG_SUCCESS, data);
    }
    
    /**
     * 构建成功响应（自定义消息）
     * 
     * @param message 消息内容
     * @param data 数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildSuccess(String message, Object data) {
        return buildSuccessResponse(ResponseCode.SUCCESS, message, data);
    }
    
    /**
     * 构建创建成功响应
     * 
     * @param data 数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildCreated(Object data) {
        return buildSuccessResponse(ResponseCode.CREATED, ResponseCode.MSG_SUCCESS, data);
    }
    
    /**
     * 构建操作成功响应
     * 
     * @param data 数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildOperationSuccess(Object data) {
        return buildSuccessResponse(ResponseCode.OPERATION_SUCCESS, ResponseCode.MSG_SUCCESS, data);
    }
    
    /**
     * 构建查询成功响应
     * 
     * @param data 数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildQuerySuccess(Object data) {
        return buildSuccessResponse(ResponseCode.SUCCESS, ResponseCode.MSG_SUCCESS, data);
    }
    
    /**
     * 构建分页查询成功响应
     * 
     * @param data 分页数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildPageSuccess(Object data) {
        return buildSuccessResponse(ResponseCode.SUCCESS, ResponseCode.MSG_SUCCESS, data);
    }
    
    /**
     * 构建导入成功响应
     * 
     * @param data 数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildImportSuccess(Object data) {
        return buildSuccessResponse(ResponseCode.IMPORT_SUCCESS, ResponseCode.MSG_IMPORT_SUCCESS, data);
    }
    
    /**
     * 构建导入完成响应
     * 
     * @param data 数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildImportCompleted(Object data) {
        return buildSuccessResponse(ResponseCode.IMPORT_COMPLETED, ResponseCode.MSG_IMPORT_COMPLETED, data);
    }
    
    // ==================== 通用失败响应方法 ====================
    
    /**
     * 构建失败响应（默认错误消息）
     * 
     * @param error 错误信息
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildError(String error) {
        return buildCustomErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, ResponseCode.MSG_FAILED, error);
    }
    
    /**
     * 构建失败响应（自定义错误消息）
     * 
     * @param message 错误消息
     * @param error 错误信息
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildError(String message, String error) {
        return buildCustomErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, message, error);
    }
    
    /**
     * 构建参数验证失败响应
     * 
     * @param field 字段名
     * @param reason 原因
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildValidationFailed(String field, String reason) {
        String message = String.format("参数验证失败: %s - %s", field, reason);
        return buildCustomErrorResponse(ResponseCode.VALIDATION_FAILED, message, null);
    }
    
    /**
     * 构建参数验证失败响应（默认消息）
     * 
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildValidationFailed() {
        return buildCustomErrorResponse(ResponseCode.VALIDATION_FAILED, ResponseCode.MSG_VALIDATION_FAILED, null);
    }
    
    /**
     * 构建资源不存在响应
     * 
     * @param resource 资源类型
     * @param id 资源ID
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildNotFound(String resource, Object id) {
        String message = String.format("%s不存在: ID=%s", resource, id);
        return buildCustomErrorResponse(ResponseCode.DATA_NOT_FOUND, message, null);
    }
    
    /**
     * 构建资源不存在响应（默认消息）
     * 
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildNotFound() {
        return buildCustomErrorResponse(ResponseCode.DATA_NOT_FOUND, ResponseCode.MSG_DATA_NOT_FOUND, null);
    }
    
    /**
     * 构建权限不足响应
     * 
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildInsufficientPermission() {
        return buildCustomErrorResponse(ResponseCode.INSUFFICIENT_PERMISSION, ResponseCode.MSG_INSUFFICIENT_PERMISSION, null);
    }
    
    /**
     * 构建操作失败响应
     * 
     * @param error 错误信息
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildOperationFailed(String error) {
        return buildCustomErrorResponse(ResponseCode.OPERATION_FAILED, ResponseCode.MSG_FAILED, error);
    }
    
    /**
     * 构建操作失败响应（默认消息）
     * 
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildOperationFailed() {
        return buildCustomErrorResponse(ResponseCode.OPERATION_FAILED, ResponseCode.MSG_FAILED, null);
    }
    
    /**
     * 构建超时响应
     * 
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildTimeout() {
        return buildCustomErrorResponse(ResponseCode.TIMEOUT, ResponseCode.MSG_TIMEOUT, null);
    }
    
    /**
     * 构建导入失败响应
     * 
     * @param error 错误信息
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildImportFailed(String error) {
        return buildCustomErrorResponse(ResponseCode.IMPORT_FAILED, ResponseCode.MSG_IMPORT_FAILED, error);
    }
    
    /**
     * 构建导入失败响应（默认消息）
     * 
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildImportFailed() {
        return buildCustomErrorResponse(ResponseCode.IMPORT_FAILED, ResponseCode.MSG_IMPORT_FAILED, null);
    }
}