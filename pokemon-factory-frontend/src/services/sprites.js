import { SPRITES_BASE } from './httpClient'

const REMOTE_FALLBACK_BASE = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites'

function buildPrimary(path) {
  return `${SPRITES_BASE}${path}`
}

function buildFallback(path) {
  return `${REMOTE_FALLBACK_BASE}${path}`
}

export const sprites = {
  pokemon: (id) => buildPrimary(`/pokemon/${id}.png`),
  official: (id) => buildPrimary(`/pokemon/other/official-artwork/${id}.png`),
  type: (id) => buildPrimary(`/types/${id}.png`),
  item: (name) => buildPrimary(`/items/${name}.png`),
  default: buildPrimary('/pokemon/0.png'),
  fallbackPokemon: (id) => buildFallback(`/pokemon/${id}.png`),
  fallbackOfficial: (id) => buildFallback(`/pokemon/other/official-artwork/${id}.png`),
  fallbackItem: (name) => buildFallback(`/items/${name}.png`),
  fallbackDefault: buildFallback('/pokemon/0.png')
}