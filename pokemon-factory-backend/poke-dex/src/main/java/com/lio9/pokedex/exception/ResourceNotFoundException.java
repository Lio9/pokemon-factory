package com.lio9.pokedex.exception;



/**
 * 资源未找到异常
 * 当请求的资源不存在时抛出
 *
 * @author Lio9
 * @version 1.0
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super("RESOURCE_NOT_FOUND", String.format("%s 不存在: %s", resourceName, resourceId));
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}