<template>
  <div class="modern-pokedex">
    <!-- 页面头部 -->
    <header class="pokedex-header">
      <div class="header-background">
        <div class="header-gradient"></div>
        <div class="header-pattern"></div>
      </div>
      
      <div class="header-content">
        <div class="header-text">
          <h1 class="header-title">
            <span class="title-icon">📚</span>
            <span class="title-text">宝可梦图鉴</span>
          </h1>
          <p class="header-description">
            探索所有世代的宝可梦，了解它们的属性、技能和特性
          </p>
        </div>
        
        <div class="header-stats">
          <div class="stat-item">
            <div class="stat-number">1025</div>
            <div class="stat-label">宝可梦</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">18</div>
            <div class="stat-label">属性</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">9</div>
            <div class="stat-label">世代</div>
          </div>
        </div>
      </div>
    </header>

    <!-- 搜索和筛选 -->
    <section class="search-section">
      <div class="search-container">
        <div class="search-box">
          <div class="search-icon">🔍</div>
          <input 
            v-model="searchQuery" 
            type="text" 
            class="search-input" 
            placeholder="搜索宝可梦名称、属性或技能..."
            @input="handleSearch"
          >
          <button v-if="searchQuery" class="clear-button" @click="clearSearch">
            <span class="clear-icon">✕</span>
          </button>
        </div>
        
        <div class="filter-buttons">
          <button 
            v-for="filter in filters" 
            :key="filter.id"
            class="filter-button"
            :class="{ active: activeFilter === filter.id }"
            @click="setFilter(filter.id)"
          >
            <span class="filter-icon">{{ filter.icon }}</span>
            <span class="filter-text">{{ filter.name }}</span>
          </button>
        </div>
      </div>
    </section>

    <!-- 宝可梦网格 -->
    <section class="pokemon-grid-section">
      <div class="grid-container">
        <div class="grid-header">
          <div class="results-count">
            <span class="count-number">{{ filteredPokemon.length }}</span>
            <span class="count-text">个宝可梦</span>
          </div>
          
          <div class="view-options">
            <button 
              class="view-button" 
              :class="{ active: viewMode === 'grid' }"
              @click="viewMode = 'grid'"
            >
              <span class="view-icon">⊞</span>
            </button>
            <button 
              class="view-button" 
              :class="{ active: viewMode === 'list' }"
              @click="viewMode = 'list'"
            >
              <span class="view-icon">☰</span>
            </button>
          </div>
        </div>
        
        <div class="pokemon-grid" :class="viewMode">
          <div 
            v-for="(pokemon, index) in paginatedPokemon" 
            :key="pokemon.id"
            class="pokemon-card"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @click="selectPokemon(pokemon)"
          >
            <div class="card-background" :style="{ background: getTypeGradient(pokemon.types) }"></div>
            
            <div class="card-content">
              <div class="pokemon-image-container">
                <img 
                  :src="pokemon.image" 
                  :alt="pokemon.name"
                  class="pokemon-image"
                  @error="handleImageError"
                >
                <div class="pokemon-number">#{{ String(pokemon.id).padStart(3, '0') }}</div>
              </div>
              
              <div class="pokemon-info">
                <h3 class="pokemon-name">{{ pokemon.name }}</h3>
                <div class="pokemon-types">
                  <span 
                    v-for="type in pokemon.types" 
                    :key="type"
                    class="type-badge"
                    :style="{ background: getTypeColor(type) }"
                  >
                    {{ type }}
                  </span>
                </div>
                <div class="pokemon-stats">
                  <div class="stat-bar">
                    <span class="stat-label">HP</span>
                    <div class="stat-progress">
                      <div class="stat-fill" :style="{ width: `${pokemon.stats.hp / 2}%`, background: getStatColor(pokemon.stats.hp) }"></div>
                    </div>
                    <span class="stat-value">{{ pokemon.stats.hp }}</span>
                  </div>
                  <div class="stat-bar">
                    <span class="stat-label">攻击</span>
                    <div class="stat-progress">
                      <div class="stat-fill" :style="{ width: `${pokemon.stats.attack / 2}%`, background: getStatColor(pokemon.stats.attack) }"></div>
                    </div>
                    <span class="stat-value">{{ pokemon.stats.attack }}</span>
                  </div>
                </div>
              </div>
              
              <div class="card-actions">
                <button class="action-button favorite" @click.stop="toggleFavorite(pokemon)">
                  <span class="action-icon">{{ pokemon.isFavorite ? '❤️' : '🤍' }}</span>
                </button>
                <button class="action-button detail" @click.stop="showDetail(pokemon)">
                  <span class="action-icon">📋</span>
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more">
          <button class="load-more-button" @click="loadMore" :disabled="loading">
            <span v-if="loading" class="loading-spinner"></span>
            <span v-else class="load-more-text">加载更多</span>
          </button>
        </div>
      </div>
    </section>

    <!-- 宝可梦详情弹窗 -->
    <div v-if="selectedPokemon" class="pokemon-detail-modal" @click.self="closeDetail">
      <div class="modal-content">
        <button class="modal-close" @click="closeDetail">
          <span class="close-icon">✕</span>
        </button>
        
        <div class="modal-header">
          <div class="modal-image-container">
            <img :src="selectedPokemon.image" :alt="selectedPokemon.name" class="modal-image">
          </div>
          <div class="modal-title">
            <h2 class="modal-name">{{ selectedPokemon.name }}</h2>
            <div class="modal-number">#{{ String(selectedPokemon.id).padStart(3, '0') }}</div>
            <div class="modal-types">
              <span 
                v-for="type in selectedPokemon.types" 
                :key="type"
                class="type-badge large"
                :style="{ background: getTypeColor(type) }"
              >
                {{ type }}
              </span>
            </div>
          </div>
        </div>
        
        <div class="modal-body">
          <div class="detail-tabs">
            <button 
              v-for="tab in tabs" 
              :key="tab.id"
              class="tab-button"
              :class="{ active: activeTab === tab.id }"
              @click="activeTab = tab.id"
            >
              <span class="tab-icon">{{ tab.icon }}</span>
              <span class="tab-text">{{ tab.name }}</span>
            </button>
          </div>
          
          <div class="tab-content">
            <!-- 基础信息 -->
            <div v-if="activeTab === 'basic'" class="tab-panel">
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">身高</span>
                  <span class="info-value">{{ selectedPokemon.height }}m</span>
                </div>
                <div class="info-item">
                  <span class="info-label">体重</span>
                  <span class="info-value">{{ selectedPokemon.weight }}kg</span>
                </div>
                <div class="info-item">
                  <span class="info-label">基础经验</span>
                  <span class="info-value">{{ selectedPokemon.base_experience }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">特性</span>
                  <span class="info-value">{{ selectedPokemon.abilities?.join(', ') }}</span>
                </div>
              </div>
            </div>
            
            <!-- 能力值 -->
            <div v-if="activeTab === 'stats'" class="tab-panel">
              <div class="stats-chart">
                <div v-for="(value, stat) in selectedPokemon.stats" :key="stat" class="chart-bar">
                  <span class="chart-label">{{ stat }}</span>
                  <div class="chart-progress">
                    <div class="chart-fill" :style="{ width: `${value / 2}%`, background: getStatColor(value) }"></div>
                  </div>
                  <span class="chart-value">{{ value }}</span>
                </div>
              </div>
            </div>
            
            <!-- 技能 -->
            <div v-if="activeTab === 'moves'" class="tab-panel">
              <div class="moves-list">
                <div v-for="move in selectedPokemon.moves" :key="move.name" class="move-item">
                  <div class="move-header">
                    <span class="move-name">{{ move.name }}</span>
                    <span class="move-type" :style="{ background: getTypeColor(move.type) }">{{ move.type }}</span>
                  </div>
                  <div class="move-stats">
                    <span class="move-stat">威力: {{ move.power || '-' }}</span>
                    <span class="move-stat">命中: {{ move.accuracy || '-' }}%</span>
                    <span class="move-stat">PP: {{ move.pp }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// 响应式状态
const searchQuery = ref('')
const activeFilter = ref('all')
const viewMode = ref('grid')
const selectedPokemon = ref(null)
const activeTab = ref('basic')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20

// 筛选器
const filters = [
  { id: 'all', name: '全部', icon: '📋' },
  { id: 'fire', name: '火', icon: '🔥' },
  { id: 'water', name: '水', icon: '💧' },
  { id: 'grass', name: '草', icon: '🌿' },
  { id: 'electric', name: '电', icon: '⚡' },
  { id: 'psychic', name: '超能', icon: '🔮' },
  { id: 'dragon', name: '龙', icon: '🐉' }
]

// 标签页
const tabs = [
  { id: 'basic', name: '基础', icon: '📊' },
  { id: 'stats', name: '能力', icon: '💪' },
  { id: 'moves', name: '技能', icon: '⚔️' }
]

// 示例数据
const pokemonList = ref([
  {
    id: 1,
    name: '妙蛙种子',
    types: ['草', '毒'],
    image: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png',
    stats: { hp: 45, attack: 49, defense: 49, sp_attack: 65, sp_defense: 65, speed: 45 },
    height: 0.7,
    weight: 6.9,
    base_experience: 64,
    abilities: ['茂盛', '叶绿素'],
    moves: [
      { name: '撞击', type: '一般', power: 40, accuracy: 100, pp: 35 },
      { name: '叫声', type: '一般', power: null, accuracy: 100, pp: 40 },
      { name: '藤鞭', type: '草', power: 45, accuracy: 100, pp: 25 }
    ],
    isFavorite: false
  },
  {
    id: 4,
    name: '小火龙',
    types: ['火'],
    image: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png',
    stats: { hp: 39, attack: 52, defense: 43, sp_attack: 60, sp_defense: 50, speed: 65 },
    height: 0.6,
    weight: 8.5,
    base_experience: 62,
    abilities: ['猛火', '太阳之力'],
    moves: [
      { name: '抓', type: '一般', power: 40, accuracy: 100, pp: 35 },
      { name: '叫声', type: '一般', power: null, accuracy: 100, pp: 40 },
      { name: '火花', type: '火', power: 40, accuracy: 100, pp: 25 }
    ],
    isFavorite: false
  },
  {
    id: 7,
    name: '杰尼龟',
    types: ['水'],
    image: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/7.png',
    stats: { hp: 44, attack: 48, defense: 65, sp_attack: 50, sp_defense: 64, speed: 43 },
    height: 0.5,
    weight: 9.0,
    base_experience: 63,
    abilities: ['激流', '雨盘'],
    moves: [
      { name: '撞击', type: '一般', power: 40, accuracy: 100, pp: 35 },
      { name: '摇尾巴', type: '一般', power: null, accuracy: 100, pp: 30 },
      { name: '水枪', type: '水', power: 40, accuracy: 100, pp: 25 }
    ],
    isFavorite: false
  },
  {
    id: 25,
    name: '皮卡丘',
    types: ['电'],
    image: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png',
    stats: { hp: 35, attack: 55, defense: 40, sp_attack: 50, sp_defense: 50, speed: 90 },
    height: 0.4,
    weight: 6.0,
    base_experience: 112,
    abilities: ['静电', '避雷针'],
    moves: [
      { name: '电击', type: '电', power: 40, accuracy: 100, pp: 30 },
      { name: '叫声', type: '一般', power: null, accuracy: 100, pp: 40 },
      { name: '电光一闪', type: '一般', power: 40, accuracy: 100, pp: 30 }
    ],
    isFavorite: true
  }
])

// 计算属性
const filteredPokemon = computed(() => {
  let result = pokemonList.value
  
  // 搜索筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(pokemon => 
      pokemon.name.toLowerCase().includes(query) ||
      pokemon.types.some(type => type.toLowerCase().includes(query))
    )
  }
  
  // 类型筛选
  if (activeFilter.value !== 'all') {
    result = result.filter(pokemon => 
      pokemon.types.some(type => type.toLowerCase() === activeFilter.value)
    )
  }
  
  return result
})

const paginatedPokemon = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return filteredPokemon.value.slice(start, end)
})

const hasMore = computed(() => {
  return paginatedPokemon.value.length < filteredPokemon.value.length
})

// 方法
function handleSearch() {
  currentPage.value = 1
}

function clearSearch() {
  searchQuery.value = ''
  currentPage.value = 1
}

function setFilter(filterId) {
  activeFilter.value = filterId
  currentPage.value = 1
}

function selectPokemon(pokemon) {
  selectedPokemon.value = pokemon
  activeTab.value = 'basic'
}

function closeDetail() {
  selectedPokemon.value = null
}

function showDetail(pokemon) {
  selectPokemon(pokemon)
}

function toggleFavorite(pokemon) {
  pokemon.isFavorite = !pokemon.isFavorite
}

function loadMore() {
  loading.value = true
  setTimeout(() => {
    currentPage.value++
    loading.value = false
  }, 500)
}

function handleImageError(e) {
  e.target.src = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/0.png'
}

function getTypeColor(type) {
  const colors = {
    '一般': '#A8A878',
    '火': '#F08030',
    '水': '#6890F0',
    '电': '#F8D030',
    '草': '#78C850',
    '冰': '#98D8D8',
    '格斗': '#C03028',
    '毒': '#A040A0',
    '地面': '#E0C068',
    '飞行': '#A890F0',
    '超能': '#F85888',
    '虫': '#A8B820',
    '岩石': '#B8A038',
    '幽灵': '#705898',
    '龙': '#7038F8',
    '恶': '#705848',
    '钢': '#B8B8D0',
    '妖精': '#EE99AC'
  }
  return colors[type] || '#68A090'
}

function getTypeGradient(types) {
  if (types.length === 1) {
    return `linear-gradient(135deg, ${getTypeColor(types[0])} 0%, ${getTypeColor(types[0])}88 100%)`
  }
  return `linear-gradient(135deg, ${getTypeColor(types[0])} 0%, ${getTypeColor(types[1])} 100%)`
}

function getStatColor(value) {
  if (value >= 150) return '#ff6b6b'
  if (value >= 100) return '#ffd93d'
  if (value >= 50) return '#6bcb77'
  return '#4d96ff'
}

onMounted(() => {
  // 初始化动画
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('animate-in')
      }
    })
  }, { threshold: 0.1 })
  
  document.querySelectorAll('.pokemon-card').forEach(el => {
    observer.observe(el)
  })
})
</script>

<style scoped>
.modern-pokedex {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

/* 页面头部 */
.pokedex-header {
  position: relative;
  padding: 4rem 2rem;
  overflow: hidden;
}

.header-background {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.header-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
}

.header-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 25% 25%, rgba(255,255,255,0.2) 0%, transparent 50%),
                     radial-gradient(circle at 75% 75%, rgba(255,255,255,0.1) 0%, transparent 50%);
}

.header-content {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    text-align: center;
    gap: 2rem;
  }
}

.header-title {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 2.5rem;
  font-weight: 800;
  color: white;
  margin-bottom: 1rem;
}

.title-icon {
  font-size: 3rem;
}

.header-description {
  font-size: 1.125rem;
  color: rgba(255, 255, 255, 0.9);
  max-width: 500px;
}

.header-stats {
  display: flex;
  gap: 2rem;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 2.5rem;
  font-weight: 800;
  color: white;
  line-height: 1;
  margin-bottom: 0.25rem;
}

.stat-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* 搜索和筛选 */
.search-section {
  padding: 2rem;
  background: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.search-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

@media (min-width: 768px) {
  .search-container {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

.search-box {
  display: flex;
  align-items: center;
  background: #f3f4f6;
  border-radius: 1rem;
  padding: 0.75rem 1rem;
  flex: 1;
  max-width: 500px;
  transition: all 0.3s ease;
}

.search-box:focus-within {
  background: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.3);
}

.search-icon {
  font-size: 1.25rem;
  margin-right: 0.75rem;
  color: #9ca3af;
}

.search-input {
  flex: 1;
  border: none;
  background: none;
  font-size: 1rem;
  color: #1f2937;
  outline: none;
}

.search-input::placeholder {
  color: #9ca3af;
}

.clear-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 0.5rem;
  transition: background 0.2s ease;
}

.clear-button:hover {
  background: #e5e7eb;
}

.clear-icon {
  font-size: 1rem;
  color: #6b7280;
}

.filter-buttons {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
  padding-bottom: 0.5rem;
}

@media (min-width: 768px) {
  .filter-buttons {
    padding-bottom: 0;
  }
}

.filter-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: #f3f4f6;
  border: none;
  border-radius: 0.75rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.filter-button:hover {
  background: #e5e7eb;
}

.filter-button.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.filter-icon {
  font-size: 1rem;
}

/* 宝可梦网格 */
.pokemon-grid-section {
  padding: 2rem;
}

.grid-container {
  max-width: 1200px;
  margin: 0 auto;
}

.grid-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.results-count {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.count-number {
  font-size: 2rem;
  font-weight: 800;
  color: #1f2937;
}

.count-text {
  font-size: 1rem;
  color: #6b7280;
}

.view-options {
  display: flex;
  gap: 0.5rem;
}

.view-button {
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: none;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.view-button:hover {
  background: #e5e7eb;
}

.view-button.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.view-icon {
  font-size: 1.25rem;
}

.pokemon-grid {
  display: grid;
  gap: 1.5rem;
}

.pokemon-grid.grid {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
}

.pokemon-grid.list {
  grid-template-columns: 1fr;
}

/* 宝可梦卡片 */
.pokemon-card {
  position: relative;
  background: white;
  border-radius: 1.5rem;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
  opacity: 0;
  transform: translateY(20px);
}

.pokemon-card.animate-in {
  opacity: 1;
  transform: translateY(0);
}

.pokemon-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.card-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 120px;
  opacity: 0.1;
}

.card-content {
  position: relative;
  z-index: 1;
  padding: 1.5rem;
}

.pokemon-image-container {
  position: relative;
  text-align: center;
  margin-bottom: 1rem;
}

.pokemon-image {
  width: 120px;
  height: 120px;
  object-fit: contain;
  filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.2));
  transition: transform 0.3s ease;
}

.pokemon-card:hover .pokemon-image {
  transform: scale(1.1);
}

.pokemon-number {
  position: absolute;
  top: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.1);
  color: rgba(255, 255, 255, 0.8);
  padding: 0.25rem 0.75rem;
  border-radius: 0 1rem 0 1rem;
  font-size: 0.75rem;
  font-weight: 600;
}

.pokemon-info {
  text-align: center;
}

.pokemon-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.75rem;
}

.pokemon-types {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.type-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 2rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.type-badge.large {
  padding: 0.375rem 1rem;
  font-size: 0.875rem;
}

.pokemon-stats {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.stat-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.stat-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #6b7280;
  min-width: 2rem;
}

.stat-progress {
  flex: 1;
  height: 0.5rem;
  background: #e5e7eb;
  border-radius: 0.25rem;
  overflow: hidden;
}

.stat-fill {
  height: 100%;
  border-radius: 0.25rem;
  transition: width 0.5s ease;
}

.stat-value {
  font-size: 0.75rem;
  font-weight: 600;
  color: #374151;
  min-width: 2rem;
  text-align: right;
}

.card-actions {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 1rem;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.pokemon-card:hover .card-actions {
  opacity: 1;
}

.action-button {
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: none;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-button:hover {
  background: #e5e7eb;
  transform: scale(1.1);
}

.action-icon {
  font-size: 1.25rem;
}

/* 加载更多 */
.load-more {
  text-align: center;
  margin-top: 3rem;
}

.load-more-button {
  padding: 1rem 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 1rem;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.load-more-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

.load-more-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.loading-spinner {
  display: inline-block;
  width: 1.25rem;
  height: 1.25rem;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 详情弹窗 */
.pokemon-detail-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 2rem;
}

.modal-content {
  background: white;
  border-radius: 2rem;
  max-width: 800px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
  animation: scaleIn 0.3s ease-out;
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-close {
  position: absolute;
  top: 1rem;
  right: 1rem;
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: none;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 10;
}

.modal-close:hover {
  background: #e5e7eb;
}

.close-icon {
  font-size: 1.25rem;
  color: #6b7280;
}

.modal-header {
  display: flex;
  gap: 2rem;
  padding: 2rem;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border-radius: 2rem 2rem 0 0;
}

@media (max-width: 640px) {
  .modal-header {
    flex-direction: column;
    text-align: center;
  }
}

.modal-image-container {
  flex-shrink: 0;
}

.modal-image {
  width: 150px;
  height: 150px;
  object-fit: contain;
  filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.2));
}

.modal-title {
  flex: 1;
}

.modal-name {
  font-size: 2rem;
  font-weight: 800;
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.modal-number {
  font-size: 1rem;
  color: #6b7280;
  margin-bottom: 1rem;
}

.modal-types {
  display: flex;
  gap: 0.5rem;
}

@media (max-width: 640px) {
  .modal-types {
    justify-content: center;
  }
}

.modal-body {
  padding: 2rem;
}

.detail-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 2rem;
  border-bottom: 2px solid #e5e7eb;
  padding-bottom: 1rem;
}

.tab-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: none;
  border: none;
  border-radius: 0.75rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-button:hover {
  background: #f3f4f6;
  color: #374151;
}

.tab-button.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.tab-icon {
  font-size: 1rem;
}

.tab-content {
  min-height: 300px;
}

.tab-panel {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
}

.info-item {
  background: #f8fafc;
  padding: 1.5rem;
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
}

.info-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.info-value {
  font-size: 1.125rem;
  font-weight: 700;
  color: #1f2937;
}

.stats-chart {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.chart-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.chart-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
  min-width: 5rem;
  text-transform: capitalize;
}

.chart-progress {
  flex: 1;
  height: 1rem;
  background: #e5e7eb;
  border-radius: 0.5rem;
  overflow: hidden;
}

.chart-fill {
  height: 100%;
  border-radius: 0.5rem;
  transition: width 0.5s ease;
}

.chart-value {
  font-size: 0.875rem;
  font-weight: 700;
  color: #1f2937;
  min-width: 3rem;
  text-align: right;
}

.moves-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.move-item {
  background: #f8fafc;
  padding: 1.5rem;
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
}

.move-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.move-name {
  font-size: 1rem;
  font-weight: 700;
  color: #1f2937;
}

.move-type {
  padding: 0.25rem 0.75rem;
  border-radius: 2rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: white;
}

.move-stats {
  display: flex;
  gap: 1.5rem;
}

.move-stat {
  font-size: 0.875rem;
  color: #6b7280;
}
</style>