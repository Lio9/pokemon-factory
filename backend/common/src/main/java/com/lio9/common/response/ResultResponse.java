package com.lio9.common.response;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一 API 响应构建器。
 *
 * <p>所有后端接口统一返回 {@code {code, message, data}} 结构。
 * 业务层抛出异常时由 {@code @RestControllerAdvice} 统一捕获并构建错误响应。</p>
 *
 * <pre>
 *   // 成功
 *   return ResultResponse.buildSuccess(data);
 *   // 分页
 *   return ResultResponse.buildPageSuccess(page);
 *   // 携带自定义消息的成功
 *   return ResultResponse.buildSuccess("消息", data);
 * </pre>
 */
public final class ResultResponse {

    private ResultResponse() {}

    /** 构建 {@code {code, message, data}} 成功响应 */
    public static Map<String, Object> buildSuccess(Object data) {
        return build(ResponseCode.SUCCESS, "操作成功", data);
    }

    /** 构建携带自定义消息的成功响应 */
    public static Map<String, Object> buildSuccess(String message, Object data) {
        return build(ResponseCode.SUCCESS, message, data);
    }

    /** 构建 201 Created 响应 */
    public static Map<String, Object> buildCreated(Object data) {
        return build(ResponseCode.CREATED, "创建成功", data);
    }

    /** 构建分页查询成功响应 */
    public static Map<String, Object> buildPageSuccess(Object data) {
        return build(ResponseCode.SUCCESS, "操作成功", data);
    }

    /** 构建自定义错误响应 */
    public static Map<String, Object> buildCustomErrorResponse(int code, String message, String error) {
        return buildCustomErrorResponse(code, message, error, null);
    }

    /** 构建带上下文的错误响应 */
    public static Map<String, Object> buildCustomErrorResponse(int code, String message, String error, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("error", error);
        if (data != null) result.put("data", data);
        return result;
    }

    /** 构建错误响应（带自定义消息和异常信息） */
    public static Map<String, Object> buildError(String message, String error) {
        return buildCustomErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, message, error);
    }

    /** 构建资源不存在响应 */
    public static Map<String, Object> buildNotFound(String resource, Object id) {
        return buildCustomErrorResponse(ResponseCode.DATA_NOT_FOUND,
                String.format("%s不存在: ID=%s", resource, id), null);
    }

    /** 构建参数验证失败响应 */
    public static Map<String, Object> buildValidationFailed(String field, String reason) {
        return buildCustomErrorResponse(ResponseCode.VALIDATION_FAILED,
                String.format("参数验证失败: %s - %s", field, reason), null);
    }

    // ── 兼容别名（部分旧控制器仍在使用） ─────────────────────────────────

    /** @deprecated 改用 {@link #buildSuccess(Object)} */
    @Deprecated public static Map<String, Object> buildSuccessResponse(int code, String message, Object data) {
        return build(code, message, data);
    }

    /** @deprecated 改用 {@link #buildSuccess(Object)} */
    @Deprecated public static Map<String, Object> buildOperationSuccess(Object data) {
        return build(ResponseCode.SUCCESS, "操作成功", data);
    }

    /** @deprecated 改用 {@link #buildError(String, String)} */
    @Deprecated public static Map<String, Object> buildOperationFailed(String error) {
        return buildCustomErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, "操作失败", error);
    }

    /** @deprecated */
    @Deprecated public static Map<String, Object> buildImportSuccess(Object data) {
        return build(ResponseCode.IMPORT_SUCCESS, "导入成功", data);
    }

    /** @deprecated */
    @Deprecated public static Map<String, Object> buildImportFailed(String error) {
        return buildCustomErrorResponse(ResponseCode.IMPORT_FAILED, "导入失败", error);
    }

    // ── 内部 ──────────────────────────────────────────────────────────────

    private static Map<String, Object> build(int code, String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);
        return result;
    }
}
