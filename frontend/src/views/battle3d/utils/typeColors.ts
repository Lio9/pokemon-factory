/**
 * typeColors.ts - 属性颜色和工具函数
 *
 * 统一管理宝可梦属性颜色映射和相关工具函数，
 * 避免在多处重复定义。
 *
 * @module utils/typeColors
 */

/**
 * 属性颜色映射（18 种属性）
 * Type color mapping (18 types)
 */
export const TYPE_COLORS: Record<string, string> = {
  Normal: '#A8A77A',
  Fire: '#EE8130',
  Water: '#6390F0',
  Electric: '#F7D02C',
  Grass: '#7AC74C',
  Ice: '#96D9D6',
  Fighting: '#C22E28',
  Poison: '#A33EA1',
  Ground: '#E2BF65',
  Flying: '#A98FF3',
  Psychic: '#F95587',
  Bug: '#A6B91A',
  Rock: '#B6A136',
  Ghost: '#735797',
  Dragon: '#6F35FC',
  Dark: '#705746',
  Steel: '#B7B7CE',
  Fairy: '#D685AD'
}

/**
 * 获取属性颜色
 * Get type color
 *
 * @param typeName - 属性名称（中文或英文）
 * @returns 颜色值
 */
export function getTypeColor(typeName: string): string {
  if (!typeName) return TYPE_COLORS.Normal

  // 尝试直接匹配
  if (TYPE_COLORS[typeName]) {
    return TYPE_COLORS[typeName]
  }

  // 尝试首字母大写匹配
  const capitalized = typeName.charAt(0).toUpperCase() + typeName.slice(1).toLowerCase()
  if (TYPE_COLORS[capitalized]) {
    return TYPE_COLORS[capitalized]
  }

  // 中文属性名映射
  const zhToEn: Record<string, string> = {
    '一般': 'Normal', '火': 'Fire', '水': 'Water', '电': 'Electric',
    '草': 'Grass', '冰': 'Ice', '格斗': 'Fighting', '毒': 'Poison',
    '地面': 'Ground', '飞行': 'Flying', '超能力': 'Psychic', '虫': 'Bug',
    '岩石': 'Rock', '幽灵': 'Ghost', '龙': 'Dragon', '恶': 'Dark',
    '钢': 'Steel', '妖精': 'Fairy'
  }

  const enType = zhToEn[typeName]
  if (enType && TYPE_COLORS[enType]) {
    return TYPE_COLORS[enType]
  }

  return TYPE_COLORS.Normal
}

/**
 * 获取属性中文名
 * Get type Chinese name
 */
export const TYPE_NAMES_ZH: Record<string, string> = {
  Normal: '一般', Fire: '火', Water: '水', Electric: '电',
  Grass: '草', Ice: '冰', Fighting: '格斗', Poison: '毒',
  Ground: '地面', Flying: '飞行', Psychic: '超能力', Bug: '虫',
  Rock: '岩石', Ghost: '幽灵', Dragon: '龙', Dark: '恶',
  Steel: '钢', Fairy: '妖精'
}

/**
 * 获取属性显示名（支持中英文）
 * Get type display name
 */
export function getTypeDisplayName(typeName: string, locale: string = 'zh-CN'): string {
  if (locale === 'zh-CN') {
    return TYPE_NAMES_ZH[typeName] || typeName
  }
  return typeName
}
