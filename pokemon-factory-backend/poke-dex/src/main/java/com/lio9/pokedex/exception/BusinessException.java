package com.lio9.pokedex.exception;



/**
 * BusinessException 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端异常处理文件。
 * 核心职责：负责定义业务异常类型或统一异常响应策略。
 * 阅读建议：建议结合控制器返回格式一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import lombok.Getter;

/**
 * 业务异常基类
 * 用于封装业务逻辑中的异常情况
 *
 * @author Lio9
 * @version 1.0
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误消息
     */
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
        this.message = message;
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}