package com.lio9.common.response;



/**
 * ImportResponse 文件说明
 * 所属模块：common 公共模块。
 * 文件类型：公共响应封装文件。
 * 核心职责：负责统一接口返回结构、响应码或导入结果表达方式。
 * 阅读建议：建议结合控制器层返回格式一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import java.util.HashMap;
import java.util.Map;

/**
 * 导入响应构建器
 * 提供导入相关的响应构建方法
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-02-15
 */
public class ImportResponse {
    
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