// API服务配置
const API_BASE = 'http://localhost:8081/api/pokedex'

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
  },
  
  // 获取伤害相关道具列表（常用战斗道具）
  getBattleItems: () => {
    const queryParams = new URLSearchParams({
      current: 1,
      size: 200
    })
    return request(`${API_BASE}/items/list?${queryParams}`)
  }
}

// 伤害计算器相关API
const DAMAGE_API_BASE = 'http://localhost:8081/api/damage'

export const damageApi = {
  // 计算伤害
  calculate: (params) => {
    return request(`${DAMAGE_API_BASE}/calculate`, {
      method: 'POST',
      body: JSON.stringify(params)
    })
  },
  
  // 获取属性相性表
  getTypeEfficacy: () => {
    return request(`${DAMAGE_API_BASE}/type-efficacy`)
  },
  
  // 获取特定属性的相性
  getTypeEfficacyByType: (typeId) => {
    return request(`${DAMAGE_API_BASE}/type-efficacy/${typeId}`)
  }
}

// 图片服务器基础URL
const SPRITES_BASE = 'http://10.144.63.175:8080/sprites'

// 图片URL生成器
export const sprites = {
  // 宝可梦图片(使用species id)
  pokemon: (id) => `${SPRITES_BASE}/pokemon/${id}.png`,
  // 宝可梦官方立绘
  official: (id) => `${SPRITES_BASE}/pokemon/other/official-artwork/${id}.png`,
  // 属性图标
  type: (id) => `${SPRITES_BASE}/types/${id}.png`,
  // 物品图标
  item: (name) => `${SPRITES_BASE}/items/${name}.png`,
  // 默认图片
  default: `${SPRITES_BASE}/pokemon/0.png`
}

export default {
  pokemon: pokemonApi,
  types: typeApi,
  abilities: abilityApi,
  moves: moveApi,
  items: itemApi,
  sprites
}
