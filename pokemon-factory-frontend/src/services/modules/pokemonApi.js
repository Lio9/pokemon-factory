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