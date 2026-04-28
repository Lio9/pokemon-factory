/*
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