package com.lio9.user.dto;



/**
 * AuthRequest 文件说明
 * 所属模块：user-module 后端模块。
 * 文件类型：数据传输对象文件。
 * 核心职责：负责在不同层之间传递精简且稳定的数据结构。
 * 阅读建议：建议关注字段是否与接口或业务流程一一对应。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

/**
 * 认证请求体。
 * <p>
 * 登录和注册当前都复用同一份入参结构，
 * 这样前后端在表单提交和接口调用上可以保持一致。
 * </p>
 */
public record AuthRequest(String username, String password) {
}
