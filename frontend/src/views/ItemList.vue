<template>
  <div class="item-list">
    <!-- 搜索栏 -->
    <div class="glass-card mb-6 p-4">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="flex-1">
          <el-input
            v-model="keyword"
            :placeholder="tr('搜索物品名称...', 'Search items...')"
            clearable
            size="large"
            class="search-input"
            @input="handleSearchInput"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button
                class="!bg-gradient-to-r !from-indigo-500 !to-purple-600 !text-white !border-none hover:!from-indigo-600 hover:!to-purple-700"
                @click="handleSearch"
              >
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
      <div class="glass-card p-5 flex items-center gap-4 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl group">
        <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xl font-bold shadow-lg transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">
          📦
        </div>
        <div>
          <div class="text-3xl font-bold text-slate-800">
            {{ total }}
          </div>
          <div class="text-xs text-slate-500 font-medium">
            {{ tr('总数', 'Total') }}
          </div>
        </div>
      </div>
      <div class="glass-card p-5 flex items-center gap-4 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl group">
        <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center text-white text-xl font-bold shadow-lg transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">
          📥
        </div>
        <div>
          <div class="text-3xl font-bold text-slate-800">
            {{ loadedCount }}
          </div>
          <div class="text-xs text-slate-500 font-medium">
            {{ tr('已加载', 'Loaded') }}
          </div>
        </div>
      </div>
      <div class="glass-card p-5 flex items-center gap-4 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl group">
        <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-sky-500 to-cyan-600 flex items-center justify-center text-white text-xl font-bold shadow-lg transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">
          🗂️
        </div>
        <div>
          <div class="text-3xl font-bold text-slate-800">
            {{ categories }}
          </div>
          <div class="text-xs text-slate-500 font-medium">
            {{ tr('分类', 'Categories') }}
          </div>
        </div>
      </div>
      <div class="glass-card p-5 flex items-center gap-4 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl group">
        <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center text-white text-xl font-bold shadow-lg transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">
          📄
        </div>
        <div>
          <div class="text-3xl font-bold text-slate-800">
            {{ pageSize }}
          </div>
          <div class="text-xs text-slate-500 font-medium">
            {{ tr('每页', 'Per page') }}
          </div>
        </div>
      </div>
    </div>

    <!-- 加载骨架 -->
    <CatalogSkeleton
      v-if="loading && items.length === 0"
      :count="12"
      :view-mode="'grid'"
    />

    <!-- 物品网格 -->
    <transition-group
      v-else-if="items.length"
      name="item-card"
      tag="div"
      class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4"
    >
      <div
        v-for="(item, index) in displayItems"
        :key="item.id"
        class="shine-effect glass-card-interactive glass-card p-4 text-center group animate-slide-up relative overflow-hidden cursor-pointer"
        :style="{ animationDelay: `${index * 25}ms` }"
        @click="showItemDetail(item)"
      >
        <div
          class="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-10"
          style="background: linear-gradient(135deg, #6366f1 20, #a855f7 40)"
        />
        <div class="relative z-10">
          <div class="aspect-square flex items-center justify-center mb-3 rounded-xl bg-gradient-to-br from-slate-50 to-slate-100 p-3 shadow-inner">
            <img
              :src="item._imageUrl"
              :alt="item.name"
              class="w-14 h-14 object-contain transition-all duration-300 group-hover:scale-125 group-hover:drop-shadow-xl float-animation"
              loading="lazy"
              @error="onImageError(item)"
            >
          </div>
          <h3 class="font-semibold text-slate-800 text-sm truncate group-hover:text-indigo-700 transition-colors">
            {{ item.name }}
          </h3>
          <p class="text-xs font-bold text-indigo-600 mt-1 bg-indigo-50 inline-block px-2 py-0.5 rounded-full">
            ¥{{ item.cost || '-' }}
          </p>
          <p
            v-if="item.description"
            class="text-xs text-slate-500 mt-2 line-clamp-2 leading-relaxed"
          >
            {{ item.description }}
          </p>
        </div>
      </div>
    </transition-group>

    <!-- 空状态 -->
    <EmptyState
      v-if="!loading && items.length === 0"
      :message="tr('没有找到物品', 'No items found')"
    />

    <!-- 加载更多 -->
    <LoadMoreTrigger
      :loading-more="loadingMore"
      :has-more="hasMore"
      :loaded-count="loadedCount"
      :total="total"
      @load-more="fetchItems(true)"
    />

    <!-- 详情弹窗 -->
    <ItemDetailDialog
      v-model:visible="showDetailDialog"
      :item="selectedItem"
    />
  </div>
</template>

<script setup>
/**
 * 物品列表页 / Item List Page
 *
 * 使用 useCatalogList 管理分页、搜索、IntersectionObserver。
 * 保留独特的统计卡片和图片处理逻辑。
 */
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { itemApi, sprites } from '../services/api.js'
import { useLocale } from '../composables/useLocale'
import { useCatalogList } from '../composables/useCatalogList'
import CatalogSkeleton from '../components/CatalogSkeleton.vue'
import LoadMoreTrigger from '../components/LoadMoreTrigger.vue'
import EmptyState from '../components/EmptyState.vue'
import ItemDetailDialog from '../components/ItemDetailDialog.vue'

const { translate: tr } = useLocale()

// ---- 图片处理 ----
function processItemData(data) {
  return data.map(item => ({
    ...item,
    _imageUrl: item.spriteUrl || getItemImage(item)
  }))
}

const FALLBACK_ITEM_URL = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items'

function getItemImage(item) {
  if (item.nameEn) {
    const name = item.nameEn.toLowerCase().replace(/[^a-z0-9]/g, '-')
    return `${FALLBACK_ITEM_URL}/${name}.png`
  }
  return sprites.default
}

function onImageError(item) {
  item._imageUrl = sprites.default
}

// ---- 通用列表逻辑（分页、搜索、Observer） ----
const {
  items,
  keyword,
  handleSearchInput,
  handleSearch,
  loading,
  loadingMore,
  hasMore,
  loadedCount,
  total,
  displayItems,
  displayCount,
  fetchItems
} = useCatalogList({
  fetchFn: async (params) => {
    const result = await itemApi.getList(params)
    if (result.code === 200) {
      result.data.records = processItemData(result.data.records || [])
    }
    return result
  },
  pageSize: 48
})

// ---- 统计 ----
const pageSize = 48
const categories = computed(() => {
  const cats = new Set(items.value.map(i => i.category).filter(Boolean))
  return cats.size || '-'
})

// ---- 详情弹窗 ----
const showDetailDialog = ref(false)
const selectedItem = ref(null)

function showItemDetail(item) {
  selectedItem.value = item
  showDetailDialog.value = true
}
</script>

<style scoped>
.item-list { padding-bottom: 1rem; }

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-slide-up {
  animation: slideUp 0.4s ease-out both;
}

/* Transition group */
.item-card-enter-active { transition: all 0.3s ease-out; }
.item-card-leave-active { transition: all 0.2s ease-in; }
.item-card-enter-from { opacity: 0; transform: scale(0.9); }
.item-card-leave-to { opacity: 0; transform: scale(0.9); }

@media (max-width: 640px) {
  .item-list { padding-bottom: 0.5rem; }
}
</style>
