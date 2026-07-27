<template>
  <div class="search-section mb-6 rounded-2xl border-3 border-slate-200/80 p-5 shadow-poke-card bg-white">
    <div class="flex flex-col lg:flex-row gap-4">
      <!-- 搜索框 -->
      <div class="flex-1">
        <el-input
          :model-value="searchKeyword"
          placeholder="搜索宝可梦名称、编号..."
          prefix-icon="Search"
          clearable
          size="large"
          class="search-input"
          @input="onSearchInput"
          @clear="onSearch"
          @keyup.enter="onSearch"
        >
          <template #append>
            <el-button
              class="!bg-poke-red !text-white !border-none hover:!bg-red-700 !font-bold"
              @click="onSearch"
            >
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>

      <!-- 属性筛选 - 圆形按钮组 -->
      <div class="w-full sm:w-auto">
        <el-select
          :model-value="selectedType"
          placeholder="属性筛选"
          clearable
          size="large"
          class="w-full"
          @change="onTypeChange"
        >
          <el-option
            v-for="type in types"
            :key="type.id"
            :label="type.name"
            :value="type.id"
          >
            <div class="flex items-center gap-2">
              <span
                class="w-4 h-4 rounded-full shadow-sm border border-white/50"
                :style="{ backgroundColor: type.color }"
              />
              <span class="font-bold">{{ type.name }}</span>
            </div>
          </el-option>
        </el-select>
      </div>

      <!-- 世代筛选 -->
      <div class="w-full sm:w-40">
        <el-select
          :model-value="selectedGeneration"
          placeholder="世代"
          clearable
          size="large"
          class="w-full"
          @change="onGenerationChange"
        >
          <el-option
            v-for="gen in generations"
            :key="gen.id"
            :label="gen.name"
            :value="gen.id"
          />
        </el-select>
      </div>

      <!-- 排序 -->
      <div class="w-full sm:w-40">
        <el-select
          :model-value="sortBy"
          placeholder="排序"
          size="large"
          class="w-full"
          @change="onSortChange"
        >
          <el-option label="图鉴编号" value="id" />
          <el-option label="名称" value="name" />
          <el-option label="攻击" value="attack" />
          <el-option label="速度" value="speed" />
        </el-select>
      </div>

      <!-- 操作按钮 -->
      <div class="flex gap-2">
        <el-button
          size="large"
          class="!rounded-xl !font-bold !border-2 !shadow-poke"
          :class="viewMode === 'grid' ? '!bg-poke-red !text-white !border-red-400' : '!bg-white !text-slate-600 !border-slate-200'"
          @click="toggleViewMode"
        >
          <component :is="viewMode === 'grid' ? 'List' : 'Grid'" class="w-4 h-4" />
          <span class="ml-1">{{ viewMode === 'grid' ? tr('列表', 'List') : tr('网格', 'Grid') }}</span>
        </el-button>
        <el-button
          size="large"
          class="!rounded-xl !font-bold !border-2 !shadow-poke"
          :class="isShowFavorites ? '!bg-amber-500 !text-white !border-amber-400' : '!bg-white !text-slate-600 !border-slate-200'"
          @click="toggleFavorites"
        >
          <component :is="isShowFavorites ? 'StarFilled' : 'Star'" class="w-4 h-4" />
          <span class="ml-1">{{ isShowFavorites ? tr('全部', 'All') : `${tr('收藏', 'Fav')} (${favorites.length})` }}</span>
        </el-button>
        <el-button
          size="large"
          class="!rounded-xl !font-bold !border-2 !bg-white !text-slate-600 !border-slate-200 !shadow-poke"
          @click="$emit('reset-filters')"
        >
          <RefreshCw class="w-4 h-4" />
          <span class="ml-1 hidden sm:inline">{{ tr('重置', 'Reset') }}</span>
        </el-button>
      </div>
    </div>

    <!-- 快速筛选标签 - Pokemon 正作风格 -->
    <div class="mt-4 flex flex-wrap gap-2">
      <button
        v-for="filter in quickFilters"
        :key="filter.key"
        class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all duration-200 border-2"
        :class="activeQuickFilters.includes(filter.key)
          ? 'bg-poke-red text-white border-red-400 shadow-poke'
          : 'bg-white text-slate-600 border-slate-200 hover:border-slate-300 hover:bg-slate-50 shadow-sm'"
        @click="$emit('toggle-quick-filter', filter.key)"
      >
        <span>{{ filter.icon }}</span>
        <span>{{ filter.label }}</span>
      </button>
    </div>
  </div>
</template>

<script>
import { Search, List, Grid, Star, StarFilled, RefreshCw } from '@element-plus/icons-vue'

export default {
  name: 'PokemonSearchFilters',
  components: { Search, List, Grid, Star, StarFilled, RefreshCw },
  props: {
    searchKeyword: { type: String, default: '' },
    selectedType: { type: [Number, String], default: null },
    selectedGeneration: { type: [Number, String], default: null },
    sortBy: { type: String, default: 'id' },
    viewMode: { type: String, default: 'grid' },
    types: { type: Array, default: () => [] },
    generations: { type: Array, default: () => [] },
    quickFilters: { type: Array, default: () => [] },
    activeQuickFilters: { type: Array, default: () => [] },
    favorites: { type: Array, default: () => [] },
    isShowFavorites: { type: Boolean, default: false }
  },
  emits: [
    'update:search-keyword',
    'update:selected-type',
    'update:selected-generation',
    'update:sort-by',
    'update:view-mode',
    'search',
    'filter',
    'toggle-quick-filter',
    'reset-filters',
    'toggle-favorites'
  ],
  methods: {
    tr(zh, en) {
      // 简单的国际化 fallback
      const locale = document.documentElement.lang || 'zh-CN'
      return locale.startsWith('zh') ? zh : en
    },
    onSearchInput(val) {
      this.$emit('update:search-keyword', val)
    },
    onSearch() {
      this.$emit('search')
    },
    onTypeChange(val) {
      this.$emit('update:selected-type', val)
      this.$emit('filter')
    },
    onGenerationChange(val) {
      this.$emit('update:selected-generation', val)
      this.$emit('filter')
    },
    onSortChange(val) {
      this.$emit('update:sort-by', val)
    },
    toggleViewMode() {
      this.$emit('update:view-mode', this.viewMode === 'grid' ? 'list' : 'grid')
    },
    toggleFavorites() {
      this.$emit('toggle-favorites')
    }
  }
}
</script>

<style scoped>
.search-section {
  transition: all 0.3s ease;
}

.search-section:hover {
  box-shadow: 0 4px 0 0 rgba(0,0,0,0.06), 0 16px 40px -8px rgba(0,0,0,0.08);
}
</style>
