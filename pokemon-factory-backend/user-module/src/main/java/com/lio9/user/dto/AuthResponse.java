package com.lio9.user.dto;

/** 认证成功响应 — 登录/注册成功后返回 access + refresh 双令牌 */
public record AuthResponse(String token, String refreshToken, UserProfile user) {}
