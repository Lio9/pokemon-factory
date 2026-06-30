<template>
  <div
    ref="listContainer"
    class="item-list"
  >
    <!-- 搜索栏 -->
    <div class="glass-card mb-6 p-4 sticky top-[4.25rem] z-10 sm:top-[4.75rem]">
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
    </div>

    <!-- 加载骨架 -->
    <div
      v-if="loading && items.length === 0"
      class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4"
    >
      <div
        v-for="i in 12"
        :key="i"
        class="glass-card p-4 animate-pulse"
      >
        <div class="aspect-square rounded-xl bg-slate-200 mb-3" />
        <div class="h-3 bg-slate-200 rounded w-3/4 mb-2" />
        <div class="h-2 bg-slate-100 rounded w-1/2" />
      </div>
    </div>

    <!-- 物品网格 -->
    <transition-group
      v-else-if="items.length"
      name="item-card"
      tag="div"
      class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4"
    >
      <div
        v-for="(item, index) in items"
        :key="item.id"
        class="shine-effect glass-card-interactive glass-card p-4 text-center group animate-slide-up relative overflow-hidden"
        :style="{ animationDelay: `${index * 25}ms` }"
      >
        <!-- 渐变背景遮罩 -->
        <div 
          class="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-10"
          style="background: linear-gradient(135deg, #6366f1 20, #a855f7 40)"
        />
        
        <div class="relative z-10">
          <div class="aspect-square flex items-center justify-center mb-3 rounded-xl bg-gradient-to-br from-slate-50 to-slate-100 p-3 shadow-inner">
            <div
              v-if="!item._imageLoaded"
              class="w-12 h-12 rounded-full bg-slate-200 animate-pulse"
            />
            <img
              v-show="item._imageLoaded"
              :src="item._imageUrl"
              :alt="item.name"
              class="w-14 h-14 object-contain transition-all duration-300 group-hover:scale-125 group-hover:drop-shadow-xl float-animation"
              loading="lazy"
              @load="item._imageLoaded = true"
              @error="handleImageError(item)"
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

    <div
      v-else
      class="text-center py-16"
    >
      <div class="text-4xl mb-4">
        🔍
      </div>
      <p class="text-slate-500">
        {{ tr('没有找到物品', 'No items found') }}
      </p>
    </div>

    <!-- 加载更多 -->
    <div
      ref="loadMoreTrigger"
      class="text-center py-8"
    >
      <div
        v-if="loadingMore"
        class="flex items-center justify-center gap-3"
      >
        <div class="loading-dots">
          <span /><span /><span />
        </div>
        <span class="text-sm text-slate-400">{{ tr('加载中...', 'Loading...') }}</span>
      </div>
      <div
        v-else-if="!hasMore && items.length > 0"
        class="text-sm text-slate-400"
      >
        {{ tr('已加载全部 {total} 个物品', 'All {total} items loaded', { total }) }}
      </div>
      <div
        v-else-if="!hasMore"
        class="text-sm text-slate-400"
      >
        {{ tr('下拉加载更多...', 'Scroll to load more...') }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { itemApi, sprites } from '../services/api.js'
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const listContainer = ref(null)
const loadMoreTrigger = ref(null)

const loading = ref(false)
const loadingMore = ref(false)
const items = ref([])
const keyword = ref('')
const currentPage = ref(0)
const pageSize = ref(48)
const total = ref(0)

let searchTimer = null
let observer = null

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
const hasMore = computed(() => currentPage.value < totalPages.value)
const loadedCount = computed(() => items.value.length)

const processItemData = (data) => {
  return data.map(item => ({
    ...item,
    _imageUrl: item.spriteUrl || getItemImage(item),
    _imageLoaded: false
  }))
}

const getItemImage = (item) => {
  if (item.nameEn) {
    return sprites.item(item.nameEn.toLowerCase().replace(/[^a-z0-9]/g, '-'))
  }
  return sprites.default
}

const handleImageError = (item) => {
  item._imageLoaded = true
  item._imageUrl = sprites.default
}

const fetchItems = async (isLoadMore = false) => {
  if (loading.value || loadingMore.value) return
  if (isLoadMore && !hasMore.value) return

  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
    currentPage.value = 0
    items.value = []
  }

  try {
    const nextPage = currentPage.value + 1
    const result = await itemApi.getList({
      current: nextPage,
      size: pageSize.value,
      keyword: keyword.value || undefined
    })
    if (result.code === 200) {
      const processedData = processItemData(result.data.records || [])
      items.value = [...items.value, ...processedData]
      total.value = result.data.total || 0
      currentPage.value = nextPage
    }
  } catch (error) {
    console.error('获取物品列表失败:', error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const handleSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => handleSearch(), 300)
}

const handleSearch = () => {
  fetchItems(false)
}

const setupObserver = () => {
  if (observer) observer.disconnect()
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting && hasMore.value && !loadingMore.value) {
          fetchItems(true)
        }
      })
    },
    { rootMargin: '200px', threshold: 0 }
  )
  if (loadMoreTrigger.value) {
    observer.observe(loadMoreTrigger.value)
  }
}

onMounted(() => {
  fetchItems(false)
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  if (observer) observer.disconnect()
  if (searchTimer) clearTimeout(searchTimer)
})

watch(() => items.value.length, () => {
  nextTick(() => {
    if (loadMoreTrigger.value && observer) {
      observer.disconnect()
      observer.observe(loadMoreTrigger.value)
    }
  })
})
</script>

<style scoped>
.item-list {
  padding-bottom: 1rem;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 加载动画 */
.loading-dots {
  display: flex;
  gap: 6px;
  justify-content: center;
  align-items: center;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-slide-up {
  animation: slideUp 0.4s ease-out both;
}

/* Transition group */
.item-card-enter-active {
  transition: all 0.3s ease-out;
}
.item-card-leave-active {
  transition: all 0.2s ease-in;
}
.item-card-enter-from {
  opacity: 0;
  transform: scale(0.9);
}
.item-card-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

@media (max-width: 640px) {
  .item-list { padding-bottom: 0.5rem; }
}
</style>
