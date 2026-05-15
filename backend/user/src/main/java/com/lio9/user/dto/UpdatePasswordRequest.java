package com.lio9.user.dto;

/** 更改密码请求 */
public record UpdatePasswordRequest(String currentPassword, String newPassword) {}
