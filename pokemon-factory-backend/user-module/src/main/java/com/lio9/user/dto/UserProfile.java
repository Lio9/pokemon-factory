package com.lio9.user.dto;

/** 返回给前端的用户资料视图，不含 passwordHash 等敏感字段 */
public record UserProfile(
        Long id,
        String username,
        String displayName,
        String email,
        String createdAt,
        String updatedAt,
        String lastLoginAt
) {}
