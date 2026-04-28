/*
 * userApi 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端接口模块文件。
 * 核心职责：负责按业务域封装请求入口，统一屏蔽接口路径与调用细节。
 * 阅读建议：建议对照 contracts 与 httpClient 一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { API_ROOT, requestData } from '../httpClient'

export const userApi = {
  login: (body) => requestData(`${API_ROOT}/user/login`, { method: 'POST', body: JSON.stringify(body) }),
  register: (body) => requestData(`${API_ROOT}/user/register`, { method: 'POST', body: JSON.stringify(body) }),
  me: () => requestData(`${API_ROOT}/user/me`)
}