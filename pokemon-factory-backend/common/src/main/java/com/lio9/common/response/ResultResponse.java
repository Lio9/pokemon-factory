package com.lio9.common.response;

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
     * 构建导入任务成功响应
     * 
     * @param taskId 任务ID
     * @param message 消息内容
     * @param importType 导入类型
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildImportTaskResponse(String taskId, String message, String importType) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", ResponseCode.IMPORTING);
        result.put("message", message);
        result.put("data", Map.of(
            "taskId", taskId,
            "statusUrl", "/api/import-optimized/import-status/" + taskId,
            "status", "running",
            "message", "正在执行导入...",
            "importType", importType
        ));
        return result;
    }
    
    /**
     * 构建导入任务完成响应
     * 
     * @param taskId 任务ID
     * @param message 消息内容
     * @param result 结果
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildImportCompleteResponse(String taskId, String message, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", ResponseCode.IMPORT_COMPLETED);
        response.put("message", message);
        response.put("data", Map.of(
            "taskId", taskId,
            "statusUrl", "/api/import-optimized/import-status/" + taskId,
            "status", "completed",
            "message", "导入已完成",
            "result", result
        ));
        return response;
    }
    
    /**
     * 构建失败响应
     * 
     * @param errorMessage 错误消息
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildErrorResponse(String errorMessage) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", ResponseCode.INTERNAL_SERVER_ERROR);
        result.put("message", errorMessage);
        return result;
    }
    
    /**
     * 构建自定义成功响应
     * 
     * @param code 状态码
     * @param message 消息
     * @param data 数据
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildCustomSuccessResponse(int code, String message, Object data) {
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
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("error", error);
        return result;
    }
    
    /**
     * 构建导入成功响应
     * 
     * @param taskId 任务ID
     * @param message 消息内容
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildImportSuccessResponse(String taskId, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", ResponseCode.IMPORT_SUCCESS);
        result.put("message", message);
        result.put("data", Map.of(
            "taskId", taskId,
            "statusUrl", "/api/import-optimized/import-status/" + taskId,
            "status", "completed",
            "message", "导入已完成",
            "importType", "import"
        ));
        return result;
    }
    
    /**
     * 构建导入失败响应
     * 
     * @param taskId 任务ID
     * @param errorMessage 错误消息
     * @return ResponseEntity对象
     */
    public static Map<String, Object> buildImportErrorResponse(String taskId, String errorMessage) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", ResponseCode.IMPORT_FAILED);
        result.put("message", errorMessage);
        result.put("data", Map.of(
            "taskId", taskId,
            "statusUrl", "/api/import-optimized/import-status/" + taskId,
            "status", "failed",
            "message", "导入失败",
            "importType", "import"
        ));
        return result;
    }
}