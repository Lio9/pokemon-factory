<template>
  <div class="search-section mb-6 rounded-2xl border border-transparent p-4 shadow-lg transition-all duration-300 hover:shadow-xl sticky top-[4.25rem] z-10 sm:mb-8 sm:p-6 sm:top-[4.75rem] card-glass">
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
              class="!bg-gradient-to-r !from-blue-500 !to-indigo-600 !text-white !border-none hover:!from-blue-600 hover:!to-indigo-700"
              @click="onSearch"
            >
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>

      <!-- 属性筛选 -->
      <div class="w-full sm:w-48">
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
                class="w-4 h-4 rounded-full shadow-sm"
                :style="{ backgroundColor: type.color }"
              />
              <span class="font-medium">{{ type.name }}</span>
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
          <el-option
            label="图鉴编号"
            value="id"
          />
          <el-option
            label="名称"
            value="name"
          />
          <el-option
            label="攻击"
            value="attack"
          />
          <el-option
            label="速度"
            value="speed"
          />
        </el-select>
      </div>

      <!-- 视图切换 -->
      <div class="flex w-full items-center justify-center gap-2 rounded-xl bg-gray-100 p-1 sm:w-auto sm:justify-start">
        <button
          class="p-2 rounded-lg transition-all duration-300"
          :class="viewMode === 'grid' ? 'bg-white shadow-md' : 'hover:bg-gray-200'"
          title="网格视图"
          @click="onViewModeChange('grid')"
        >
          <Grid class="w-5 h-5" />
        </button>
        <button
          class="p-2 rounded-lg transition-all duration-300"
          :class="viewMode === 'list' ? 'bg-white shadow-md' : 'hover:bg-gray-200'"
          title="列表视图"
          @click="onViewModeChange('list')"
        >
          <List class="w-5 h-5" />
        </button>
      </div>
    </div>

    <!-- 快速筛选标签 -->
    <div class="mt-4 flex flex-wrap gap-2">
      <button
        v-for="quickFilter in quickFilters"
        :key="quickFilter.key"
        class="px-3 py-1.5 rounded-full text-sm font-medium transition-all duration-300"
        :class="activeQuickFilters.includes(quickFilter.key)
          ? 'bg-gradient-to-r from-purple-500 to-pink-500 text-white shadow-md'
          : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        @click="onToggleQuickFilter(quickFilter.key)"
      >
        {{ quickFilter.icon }} {{ quickFilter.label }}
      </button>
    </div>
  </div>
</template>

<script>
import { ref, onUnmounted } from 'vue'
import { Search, Grid, List } from '@element-plus/icons-vue'

export default {
  name: 'PokemonSearchFilters',
  components: { Search, Grid, List },
  props: {
    searchKeyword: { type: String, default: '' },
    selectedType: { type: Number, default: null },
    selectedGeneration: { type: Number, default: null },
    sortBy: { type: String, default: 'id' },
    viewMode: { type: String, default: 'grid' },
    types: { type: Array, default: () => [] },
    generations: { type: Array, default: () => [] },
    quickFilters: { type: Array, default: () => [] },
    activeQuickFilters: { type: Array, default: () => [] }
  },
  emits: [
    'update:searchKeyword',
    'update:selectedType',
    'update:selectedGeneration',
    'update:sortBy',
    'update:viewMode',
    'search',
    'filter',
    'toggleQuickFilter',
    'resetFilters'
  ],
  setup() {
    let searchTimer = null

    onUnmounted(() => {
      if (searchTimer) clearTimeout(searchTimer)
    })

    return { searchTimer }
  },
  methods: {
    onSearchInput(value) {
      this.$emit('update:searchKeyword', value)
      // 防抖处理
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        this.$emit('search')
      }, 300)
    },
    onSearch() {
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.$emit('search')
    },
    onTypeChange(value) {
      this.$emit('update:selectedType', value)
      this.$emit('filter')
    },
    onGenerationChange(value) {
      this.$emit('update:selectedGeneration', value)
      this.$emit('filter')
    },
    onSortChange(value) {
      this.$emit('update:sortBy', value)
    },
    onViewModeChange(mode) {
      this.$emit('update:viewMode', mode)
    },
    onToggleQuickFilter(key) {
      this.$emit('toggleQuickFilter', key)
    }
  }
}
</script>