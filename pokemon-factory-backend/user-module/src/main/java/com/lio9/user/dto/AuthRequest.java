package com.lio9.user.dto;

/**
 * 认证请求体。
 * <p>
 * 登录和注册当前都复用同一份入参结构，
 * 这样前后端在表单提交和接口调用上可以保持一致。
 * </p>
 */
public record AuthRequest(String username, String password) {
}
