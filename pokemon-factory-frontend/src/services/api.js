// API服务配置
const API_BASE = 'http://localhost:8080/api/pokedex'

// 统一的请求处理
async function request(url, options = {}) {
  try {
    const response = await fetch(url, {
      headers: { 'Content-Type': 'application/json', ...options.headers },
      ...options
    })
    
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)
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
      size: params.size || 24,
      ...(params.typeId && { typeId: params.typeId }),
      ...(params.generationId && { generationId: params.generationId }),
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/pokemon/list?${queryParams}`)
  },
  
  // 获取宝可梦详情
  getDetail: (id) => {
    return request(`${API_BASE}/pokemon/${id}`)
  },
  
  // 获取形态技能
  getFormMoves: (formId, versionGroupId) => {
    const queryParams = new URLSearchParams({
      ...(versionGroupId && { versionGroupId })
    })
    return request(`${API_BASE}/form/${formId}/moves?${queryParams}`)
  }
}

// 属性相关API
export const typeApi = {
  // 获取所有属性
  getAll: () => {
    return request(`${API_BASE}/types`)
  }
}

// 特性相关API
export const abilityApi = {
  // 获取特性列表
  getList: (params) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/abilities/list?${queryParams}`)
  }
}

// 技能相关API
export const moveApi = {
  // 获取技能列表
  getList: (params) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.typeId && { typeId: params.typeId }),
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/moves/list?${queryParams}`)
  }
}

// 物品相关API
export const itemApi = {
  // 获取物品列表
  getList: (params) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.categoryId && { categoryId: params.categoryId }),
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/items/list?${queryParams}`)
  }
}

// 图片URL生成器
export const sprites = {
  // 宝可梦图片(使用species id)
  pokemon: (id) => `/sprites/pokemon/${id}.png`,
  // 宝可梦官方立绘
  official: (id) => `/sprites/pokemon/other/official-artwork/${id}.png`,
  // 属性图标
  type: (id) => `/sprites/types/${id}.png`,
  // 物品图标
  item: (name) => `/sprites/items/${name}.png`,
  // 默认图片
  default: '/sprites/pokemon/0.png'
}

export default {
  pokemon: pokemonApi,
  types: typeApi,
  abilities: abilityApi,
  moves: moveApi,
  items: itemApi,
  sprites
}
