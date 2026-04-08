package com.lio9.user.dto;

/**
 * 认证成功响应体。
 * <p>
 * 后端在登录或注册成功后一次性返回 token 和用户资料，
 * 前端即可立即建立本地会话，无需再额外发一次用户查询请求。
 * </p>
 */
public record AuthResponse(String token, UserProfile user) {
}
