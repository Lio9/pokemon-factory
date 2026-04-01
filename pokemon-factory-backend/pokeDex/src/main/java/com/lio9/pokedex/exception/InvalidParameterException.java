package com.lio9.pokedex.exception;

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