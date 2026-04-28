/*
 * pokemonApi 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端接口模块文件。
 * 核心职责：负责按业务域封装请求入口，统一屏蔽接口路径与调用细节。
 * 阅读建议：建议对照 contracts 与 httpClient 一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { API_BASE, API_ROOT, request } from '../httpClient'

export const pokemonApi = {
  getList: (params = {}) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 24,
      ...(params.typeId && { typeId: params.typeId }),
      ...(params.generationId && { generationId: params.generationId }),
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/pokemon/list?${queryParams}`)
  },

  getDetail: (id) => request(`${API_BASE}/pokemon/${id}`),
  getMoves: (id) => request(`${API_ROOT}/pokemon/${id}/moves`),
  getEvolutionChain: (id) => request(`${API_ROOT}/pokemon/${id}/evolution`),
  getAbilities: (id) => request(`${API_BASE}/pokemon/${id}`).then((result) => ({
    code: result.code,
    message: result.message,
    data: result.data?.abilities || []
  })),
  getFormMoves: (formId, versionGroupId) => {
    const queryParams = new URLSearchParams({
      ...(versionGroupId && { versionGroupId })
    })
    return request(`${API_BASE}/form/${formId}/moves?${queryParams}`)
  }
}