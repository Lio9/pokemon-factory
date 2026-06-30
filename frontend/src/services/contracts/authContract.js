/*
 * ============================================================
 * 认证契约 / Auth Contract
 * ============================================================
 *
 * 用户认证相关数据结构的标准化和验证。
 * Auth data model normalization and validation.
 *
 * 包含 / Includes:
 * - 登录/注册请求响应格式化 / Login/Register request/response format
 * - 用户资料标准化 / User profile normalization
 * - Token 存储结构 / Token storage structure
 * authContract 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端接口契约文件。
 * 核心职责：负责约束接口数据结构、字段命名和适配规则。
 * 阅读建议：建议重点关注字段标准化与容错处理逻辑。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

export function normalizeAuthSession(payload) {
  const session = payload && typeof payload === 'object' ? payload : {}
  return {
    token: session.token || '',
    user: session.user || null
  }
}
