package com.lio9.pokedex.exception;



/**
 * InvalidParameterException 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端异常处理文件。
 * 核心职责：负责定义业务异常类型或统一异常响应策略。
 * 阅读建议：建议结合控制器返回格式一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

/**
 * 无效参数异常
 * 当参数验证失败时抛出
 *
 * @author Lio9
 * @version 1.0
 */
public class InvalidParameterException extends BusinessException {

    public InvalidParameterException(String paramName, Object paramValue) {
        super("INVALID_PARAMETER", String.format("参数 %s 的值无效: %s", paramName, paramValue));
    }

    public InvalidParameterException(String message) {
        super("INVALID_PARAMETER", message);
    }
}