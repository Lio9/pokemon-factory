/*
 * ============================================================
 * 宝可梦数据契约 / Pokemon Data Contract
 * ============================================================
 *
 * 宝可梦相关数据结构的标准化和验证。
 * Pokemon data model normalization and validation.
 *
 * 包含 / Includes:
 * - 宝可梦详情标准化 / Pokemon detail normalization
 * - 列表数据格式化 / List data formatting
 * - 招式/特性集合处理 / Move/Ability collection handling
 * pokemonContract 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端接口契约文件。
 * 核心职责：负责约束接口数据结构、字段命名和适配规则。
 * 阅读建议：建议重点关注字段标准化与容错处理逻辑。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { assertSuccessResponse, unwrapApiData } from './apiEnvelope'

export function normalizePokemonDetail(payload) {
  return unwrapApiData(assertSuccessResponse(payload, '获取详情失败'), null)
}

export function normalizePokemonCollection(payload, fallback = []) {
  return unwrapApiData(assertSuccessResponse(payload, '获取列表失败'), fallback)
}
