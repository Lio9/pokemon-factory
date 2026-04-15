import { API_ROOT, requestData } from '../httpClient'

export const userApi = {
  login: (body) => requestData(`${API_ROOT}/user/login`, { method: 'POST', body: JSON.stringify(body) }),
  register: (body) => requestData(`${API_ROOT}/user/register`, { method: 'POST', body: JSON.stringify(body) }),
  me: () => requestData(`${API_ROOT}/user/me`)
}