package com.lio9.user.dto;



/**
 * 返回给前端的用户资料视图。
 * <p>
 * 这里只保留页面展示和登录态恢复需要的字段，
 * 明确不包含 passwordHash 之类的敏感信息。
 * </p>
 */
public record UserProfile(
        Long id,
        String username,
        String displayName,
        String createdAt,
        String updatedAt,
        String lastLoginAt
) {
}
