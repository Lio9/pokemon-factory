import { assertSuccessResponse, unwrapApiData } from './apiEnvelope'

export function normalizePokemonDetail(payload) {
  return unwrapApiData(assertSuccessResponse(payload, '获取详情失败'), null)
}

export function normalizePokemonCollection(payload, fallback = []) {
  return unwrapApiData(assertSuccessResponse(payload, '获取列表失败'), fallback)
}