package com.lio9.user.dto;



/**
 * UserProfile 文件说明
 * 所属模块：user-module 后端模块。
 * 文件类型：数据传输对象文件。
 * 核心职责：负责在不同层之间传递精简且稳定的数据结构。
 * 阅读建议：建议关注字段是否与接口或业务流程一一对应。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
