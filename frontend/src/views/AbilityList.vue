<template>
  <div class="ability-list">
    <!-- 搜索和筛选 -->
    <div class="glass-card mb-6 p-4">
      <div class="flex flex-col gap-4">
        <div class="flex flex-wrap gap-3">
          <div class="flex-1 min-w-[200px]">
            <el-input
              v-model="keyword"
              :placeholder="tr('搜索特性名称...', 'Search abilities...')"
              clearable
              size="large"
              @input="handleSearchInput"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button
                  class="!bg-gradient-to-r !from-violet-500 !to-purple-600 !text-white !border-none hover:!from-violet-600 hover:!to-purple-700"
                  @click="handleSearch"
                >
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
          <div class="flex gap-2">
            <el-button
              size="large"
              :class="viewMode === 'grid' ? 'bg-gradient-to-r from-violet-500 to-purple-600 text-white border-none' : 'bg-white text-slate-600 border-slate-300'"
              @click="toggleViewMode"
            >
              <component
                :is="viewMode === 'grid' ? 'List' : 'Grid'"
                class="w-4 h-4"
              />
              <span class="ml-1">{{ viewMode === 'grid' ? tr('列表', 'List') : tr('网格', 'Grid') }}</span>
            </el-button>
            <el-button
              size="large"
              :class="isShowFavorites ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-white border-none' : 'bg-white text-slate-600 border-slate-300'"
              @click="toggleFavorites"
            >
              <component
                :is="isShowFavorites ? 'StarFilled' : 'Star'"
                class="w-4 h-4"
              />
              <span class="ml-1">{{ isShowFavorites ? tr('全部', 'All') : `${tr('收藏', 'Fav')} (${favorites.size})` }}</span>
            </el-button>
            <el-button
              size="large"
              class="bg-white text-slate-600 border-slate-300"
              @click="showFilters = !showFilters"
            >
              <svg
                class="w-4 h-4"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              ><path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"
              /></svg>
              <span class="ml-1">{{ tr('筛选', 'Filter') }}</span>
            </el-button>
          </div>
        </div>

        <!-- 高级筛选 -->
        <el-collapse-transition>
          <div
            v-if="showFilters"
            class="flex flex-wrap gap-3 items-end p-4 rounded-xl bg-slate-50/80 border border-slate-100"
          >
            <div class="flex-1 min-w-[160px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('代数筛选', 'Generation') }}</label>
              <el-select
                v-model="selectedGeneration"
                :placeholder="tr('世代', 'Gen')"
                clearable
                size="default"
                class="w-full"
                @change="applyFilters"
              >
                <el-option
                  v-for="g in 9"
                  :key="g"
                  :label="`${tr('第', 'Gen')} ${g} ${tr('世代', '')}`"
                  :value="String(g)"
                />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('描述长度', 'Desc length') }}</label>
              <el-select
                v-model="descriptionLength"
                :placeholder="tr('描述长度', 'Length')"
                clearable
                size="default"
                class="w-full"
                @change="applyFilters"
              >
                <el-option
                  :label="tr('简短 (<50字)', 'Short (<50)')"
                  value="short"
                />
                <el-option
                  :label="tr('中等 (50-100字)', 'Medium (50-100)')"
                  value="medium"
                />
                <el-option
                  :label="tr('详细 (>100字)', 'Long (>100)')"
                  value="long"
                />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('排序', 'Sort') }}</label>
              <el-select
                v-model="sortBy"
                size="default"
                class="w-full"
                @change="applyFilters"
              >
                <el-option
                  :label="tr('默认', 'Default')"
                  value="default"
                />
                <el-option
                  label="A-Z"
                  value="name-asc"
                />
                <el-option
                  label="Z-A"
                  value="name-desc"
                />
              </el-select>
            </div>
          </div>
        </el-collapse-transition>
      </div>
    </div>

    <!-- 统计栏 -->
    <div class="flex items-center justify-between mb-4 px-1">
      <div class="text-sm text-slate-500">
        {{ tr('共 {total} 个特性', '{total} abilities total', { total }) }}
        <span
          v-if="isShowFavorites"
          class="ml-2 text-amber-600 font-medium"
        >· {{ tr('收藏', 'Favorites') }}</span>
      </div>
      <div class="text-xs text-slate-400">
        {{ tr('已加载 {count}', 'Loaded {count}', { count: loadedCount }) }}
      </div>
    </div>

    <!-- 加载骨架 -->
    <CatalogSkeleton
      v-if="loading && items.length === 0"
      :count="8"
      :view-mode="viewMode"
    />

    <!-- 网格视图 -->
    <template v-if="viewMode === 'grid' && !loading">
      <div
        v-if="filteredItems.length"
        class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4"
      >
        <div
          v-for="(ability, index) in filteredItems"
          :key="ability.id"
          class="shine-effect glass-card-interactive glass-card p-5 cursor-pointer animate-slide-up relative overflow-hidden group"
          :style="{ animationDelay: `${index * 30}ms` }"
          @click="showDetail(ability)"
        >
          <div
            class="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-10"
            style="background: linear-gradient(135deg, #8b5cf6 20, #a855f7 40)"
          />
          <div class="relative z-10">
            <div class="flex items-start justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center text-white text-sm shadow-lg transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">
                  ✦
                </div>
                <h3 class="font-semibold text-slate-800 group-hover:text-purple-700 transition-colors">
                  {{ ability.name }}
                </h3>
              </div>
              <FavoriteButton
                :is-favorited="favorites.has(ability.id)"
                size="sm"
                @toggle="toggleFavorite(ability)"
              />
            </div>
            <p class="text-sm text-slate-600 leading-relaxed line-clamp-3">
              {{ ability.description || ability.effect || '' }}
            </p>
            <div
              v-if="ability.generation"
              class="mt-3"
            >
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-gradient-to-r from-violet-100 to-purple-100 text-violet-600 shadow-sm">
                {{ tr('第 {gen} 世代', 'Gen {gen}', { gen: ability.generation }) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 列表视图 -->
    <template v-if="viewMode === 'list' && !loading">
      <div
        v-if="filteredItems.length"
        class="space-y-2"
      >
        <div
          v-for="(ability, index) in filteredItems"
          :key="ability.id"
          class="glass-card-interactive glass-card p-4 flex items-center gap-4 cursor-pointer animate-slide-up"
          :style="{ animationDelay: `${index * 20}ms` }"
          @click="showDetail(ability)"
        >
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center text-white text-sm shadow-sm flex-shrink-0">
            ✦
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <h3 class="font-semibold text-slate-800">
                {{ ability.name }}
              </h3>
              <span
                v-if="ability.generation"
                class="inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 text-slate-500"
              >{{ tr('Gen {gen}', 'Gen {gen}', { gen: ability.generation }) }}</span>
            </div>
            <p class="text-sm text-slate-500 mt-1 truncate">
              {{ ability.description || ability.effect || '' }}
            </p>
          </div>
          <FavoriteButton
            :is-favorited="favorites.has(ability.id)"
            size="lg"
            @toggle="toggleFavorite(ability)"
          />
        </div>
      </div>
    </template>

    <!-- 空状态 -->
    <EmptyState
      v-if="!loading && filteredItems.length === 0"
      :message="tr('没有找到特性', 'No abilities found')"
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
    <el-dialog
      v-model="showDetailDialog"
      :title="selectedAbility?.name"
      width="520px"
      :close-on-click-modal="true"
      destroy-on-close
      class="detail-dialog"
    >
      <div
        v-if="selectedAbility"
        class="space-y-5"
      >
        <div class="flex items-center gap-2">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center text-white text-lg shadow-sm">
            ✦
          </div>
          <div>
            <h3 class="font-semibold text-slate-800 text-lg">
              {{ selectedAbility.name }}
            </h3>
            <span
              v-if="selectedAbility.generation"
              class="text-xs text-slate-400"
            >{{ tr('第 {gen} 世代引入', 'Introduced in Gen {gen}', { gen: selectedAbility.generation }) }}</span>
          </div>
        </div>
        <div class="rounded-xl bg-slate-50 p-4 text-sm text-slate-700 leading-relaxed">
          <div class="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
            {{ tr('效果', 'Effect') }}
          </div>
          {{ selectedAbility.description || selectedAbility.effect || tr('暂无描述', 'No description') }}
        </div>
        <div
          v-if="selectedAbility.effect && selectedAbility.effect !== selectedAbility.description"
          class="rounded-xl bg-violet-50 p-4 text-sm text-violet-700 leading-relaxed"
        >
          <div class="text-xs font-semibold uppercase tracking-wider text-violet-500 mb-2">
            {{ tr('详细效果', 'Detailed effect') }}
          </div>
          {{ selectedAbility.effect }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 特性列表页 / Ability List Page
 *
 * 使用 useCatalogList 管理通用列表逻辑（分页、收藏、视图、IntersectionObserver），
 * 保留客户端筛选和详情弹窗的独特逻辑。
 */
import { ref, computed, watch } from 'vue'
import { abilityApi } from '../services/api.js'
import { useLocale } from '../composables/useLocale'
import { useCatalogList } from '../composables/useCatalogList'
import CatalogSkeleton from '../components/CatalogSkeleton.vue'
import LoadMoreTrigger from '../components/LoadMoreTrigger.vue'
import EmptyState from '../components/EmptyState.vue'
import FavoriteButton from '../components/FavoriteButton.vue'

const { translate: tr } = useLocale()

// ---- 通用列表逻辑（分页、收藏、视图、Observer） ----
const {
  items,
  keyword,
  handleSearchInput,
  handleSearch,
  favorites,
  toggleFavorite,
  isShowFavorites,
  toggleFavorites,
  viewMode,
  toggleViewMode,
  loading,
  loadingMore,
  hasMore,
  loadedCount,
  total,
  displayCount,
  fetchItems
} = useCatalogList({
  fetchFn: abilityApi.getList,
  favoritesKey: 'ability-favorites',
  pageSize: 36
})

// ---- 筛选状态 ----
const showFilters = ref(false)
const selectedGeneration = ref('')
const descriptionLength = ref('')
const sortBy = ref('default')

// ---- 筛选后的数据 ----
const filteredItems = ref([])

function applyFilters() {
  let result = [...items.value]

  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    result = result.filter(a =>
      a.name.toLowerCase().includes(kw) ||
      a.nameEn?.toLowerCase().includes(kw) ||
      a.description?.toLowerCase().includes(kw)
    )
  }

  if (selectedGeneration.value) {
    result = result.filter(a => String(a.generation) === selectedGeneration.value)
  }

  if (descriptionLength.value) {
    result = result.filter(a => {
      const len = (a.description || a.effect || '').length
      if (descriptionLength.value === 'short') return len < 50
      if (descriptionLength.value === 'medium') return len >= 50 && len <= 100
      if (descriptionLength.value === 'long') return len > 100
      return true
    })
  }

  if (isShowFavorites.value) {
    result = result.filter(a => favorites.value.has(a.id))
  }

  if (sortBy.value !== 'default') {
    const [field, order] = sortBy.value.split('-')
    result.sort((a, b) => {
      const valA = String(a[field] ?? '')
      const valB = String(b[field] ?? '')
      return order === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA)
    })
  }

  filteredItems.value = result.slice(0, displayCount.value)
}

// items 或 displayCount 变化时重新筛选（支持懒加载）
watch(() => items.value.length, () => { applyFilters() })
watch(() => displayCount.value, () => { applyFilters() })
watch([selectedGeneration, descriptionLength, sortBy, isShowFavorites], () => { applyFilters() })

// ---- 详情弹窗 ----
const showDetailDialog = ref(false)
const selectedAbility = ref(null)

function showDetail(ability) {
  selectedAbility.value = ability
  showDetailDialog.value = true
}

// 生命周期由 useCatalogList 自动管理
</script>

<style scoped>
.ability-list { padding-bottom: 1rem; }

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
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

:deep(.detail-dialog .el-dialog) { border-radius: 1.5rem !important; }
:deep(.detail-dialog .el-dialog__header) { padding: 1.5rem 1.5rem 0; }
:deep(.detail-dialog .el-dialog__body) { padding: 1.5rem; }
:deep(.detail-dialog .el-dialog__title) { font-weight: 700; font-size: 1.25rem; }

@media (max-width: 640px) {
  .ability-list { padding-bottom: 0.5rem; }
}
</style>
