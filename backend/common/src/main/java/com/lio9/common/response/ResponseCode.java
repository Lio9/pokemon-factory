package com.lio9.common.response;

/**
 * API 响应状态码。
 *
 * <p>成功时返回 {@link #SUCCESS}，业务异常由 {@link GlobalExceptionHandler}
 * 映射到对应的 HTTP 状态。</p>
 */
public final class ResponseCode {

    private ResponseCode() {}

    // ── HTTP 语义状态码 ────────────────────────────────────────────────────

    /** 通用成功 */
    public static final int SUCCESS = 200;
    /** 创建成功 */
    public static final int CREATED = 201;
    /** 请求参数错误 */
    public static final int BAD_REQUEST = 400;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 服务器内部错误 */
    public static final int INTERNAL_SERVER_ERROR = 500;

    // ── 业务状态码 ─────────────────────────────────────────────────────────

    /** 数据不存在 */
    public static final int DATA_NOT_FOUND = 1003;
    /** 参数验证失败 */
    public static final int VALIDATION_FAILED = 1004;

    // ── 导入状态码（仅 ImportResponse 使用） ──────────────────────────────

    /** 导入进行中 */
    public static final int IMPORTING = 2002;
    /** 导入完成 */
    public static final int IMPORT_COMPLETED = 2003;
    /** 导入成功 */
    public static final int IMPORT_SUCCESS = 2000;
    /** 导入失败 */
    public static final int IMPORT_FAILED = 2001;
}
