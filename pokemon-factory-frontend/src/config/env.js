const DEFAULT_ENV = Object.freeze({
  apiBase: '/api/pokedex',
  damageApiBase: '/api/damage',
  spritesBase: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites'
})

function normalizeBaseUrl(value, fallback) {
  const normalized = value && String(value).trim()
  return normalized ? normalized.replace(/\/$/, '') : fallback
}

export const appEnv = Object.freeze({
  apiBase: normalizeBaseUrl(import.meta.env.VITE_API_BASE, DEFAULT_ENV.apiBase),
  damageApiBase: normalizeBaseUrl(import.meta.env.VITE_DAMAGE_API_BASE, DEFAULT_ENV.damageApiBase),
  spritesBase: normalizeBaseUrl(import.meta.env.VITE_SPRITES_BASE, DEFAULT_ENV.spritesBase)
})

export { normalizeBaseUrl, DEFAULT_ENV }