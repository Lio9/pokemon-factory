package com.lio9.user.dto;

/** 认证请求体 — 登录和注册复用同一结构 */
public record AuthRequest(String username, String password) {}
