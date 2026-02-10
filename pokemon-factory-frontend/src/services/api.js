// API服务配置
// 创建人: Lio9

const API_BASE = 'http://localhost:8080/api'

// 统一的请求处理
async function request(url, options = {}) {
  try {
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      },
      ...options
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    return await response.json()
  } catch (error) {
    console.error('API请求失败:', error)
    throw error
  }
}

// 宝可梦相关API
export const pokemonApi = {
  // 获取宝可梦列表
  getList: (params) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.name && { name: params.name })
    })
    return request(`${API_BASE}/pokemon/list?${queryParams}`)
  },
  
  // 获取宝可梦详情
  getDetail: (id) => {
    return request(`${API_BASE}/pokemon/${id}`)
  },
  
  // 搜索宝可梦
  search: (keyword, current = 1, size = 20) => {
    const queryParams = new URLSearchParams({
      keyword,
      current,
      size
    })
    return request(`${API_BASE}/pokemon/search?${queryParams}`)
  },
  
  // 根据编号获取宝可梦
  getByIndexNumber: (indexNumber) => {
    return request(`${API_BASE}/pokemon/number/${indexNumber}`)
  },
  
  // 获取进化链
  getEvolutionChain: (id) => {
    return request(`${API_BASE}/pokemon/${id}/evolution`)
  }
}

// 招式相关API
export const moveApi = {
  // 获取招式列表
  getList: (params) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.name && { name: params.name }),
      ...(params.type && { type: params.type }),
      ...(params.category && { category: params.category })
    })
    return request(`${API_BASE}/moves/list?${queryParams}`)
  },
  
  // 获取招式详情
  getDetail: (id) => {
    return request(`${API_BASE}/moves/${id}`)
  },
  
  // 搜索招式
  search: (keyword, current = 1, size = 20) => {
    const queryParams = new URLSearchParams({
      keyword,
      current,
      size
    })
    return request(`${API_BASE}/moves/search?${queryParams}`)
  }
}

// 特性相关API
export const abilityApi = {
  // 获取特性列表
  getList: (params) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.name && { name: params.name })
    })
    return request(`${API_BASE}/abilities/list?${queryParams}`)
  },
  
  // 获取特性详情
  getDetail: (id) => {
    return request(`${API_BASE}/abilities/${id}`)
  },
  
  // 搜索特性
  search: (keyword, current = 1, size = 20) => {
    const queryParams = new URLSearchParams({
      keyword,
      current,
      size
    })
    return request(`${API_BASE}/abilities/search?${queryParams}`)
  }
}

// 物品相关API
export const itemApi = {
  // 获取物品列表
  getList: (params) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.name && { name: params.name }),
      ...(params.category && { category: params.category })
    })
    return request(`${API_BASE}/items/list?${queryParams}`)
  },
  
  // 获取物品详情
  getDetail: (id) => {
    return request(`${API_BASE}/items/${id}`)
  },
  
  // 搜索物品
  search: (keyword, current = 1, size = 20) => {
    const queryParams = new URLSearchParams({
      keyword,
      current,
      size
    })
    return request(`${API_BASE}/items/search?${queryParams}`)
  }
}

export default {
  pokemon: pokemonApi,
  moves: moveApi,
  abilities: abilityApi,
  items: itemApi
}