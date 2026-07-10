<template>
  <div class="move-list">
    <!-- 搜索和筛选 -->
    <div class="glass-card mb-6 p-4">
      <div class="flex flex-col gap-4">
        <div class="flex flex-wrap gap-3">
          <div class="flex-1 min-w-[200px]">
            <el-input
              v-model="keyword"
              :placeholder="tr('搜索技能名称...', 'Search moves...')"
              clearable
              size="large"
              @input="handleSearchInput"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button
                  class="!bg-gradient-to-r !from-rose-500 !to-orange-500 !text-white !border-none hover:!from-rose-600 hover:!to-orange-600"
                  @click="handleSearch"
                >
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
          <div class="w-full sm:w-32">
            <el-select
              v-model="selectedType"
              :placeholder="tr('属性', 'Type')"
              clearable
              size="large"
              class="w-full"
              @change="handleSearch"
            >
              <el-option
                v-for="t in types"
                :key="t.id"
                :label="t.name"
                :value="t.id"
              >
                <div class="flex items-center gap-2">
                  <span
                    class="w-3 h-3 rounded-full"
                    :style="{ backgroundColor: t.color || '#888' }"
                  />
                  <span>{{ t.name }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
          <div class="w-full sm:w-32">
            <el-select
              v-model="selectedDamageClass"
              :placeholder="tr('分类', 'Category')"
              clearable
              size="large"
              class="w-full"
              @change="handleSearch"
            >
              <el-option
                :label="tr('物理', 'Physical')"
                value="physical"
              />
              <el-option
                :label="tr('特殊', 'Special')"
                value="special"
              />
              <el-option
                :label="tr('变化', 'Status')"
                value="status"
              />
            </el-select>
          </div>
          <div class="flex gap-2">
            <el-button
              size="large"
              :class="viewMode === 'grid' ? 'bg-gradient-to-r from-rose-500 to-orange-500 text-white border-none' : 'bg-white text-slate-600 border-slate-300'"
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
              <span class="ml-1">{{ isShowFavorites ? tr('全部', 'All') : `${tr('收藏', 'Fav')} (${favorites.length})` }}</span>
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
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('威力范围', 'Power range') }}</label>
              <el-select
                v-model="powerRange"
                :placeholder="tr('威力', 'Power')"
                clearable
                size="default"
                class="w-full"
                @change="handleSearch"
              >
                <el-option
                  :label="tr('无威力', 'No power')"
                  value="none"
                />
                <el-option
                  label="1-40"
                  value="1-40"
                />
                <el-option
                  label="41-70"
                  value="41-70"
                />
                <el-option
                  label="71-100"
                  value="71-100"
                />
                <el-option
                  label="101-150"
                  value="101-150"
                />
                <el-option
                  :label="tr('151+', '151+')"
                  value="151+"
                />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('命中范围', 'Accuracy') }}</label>
              <el-select
                v-model="accuracyRange"
                :placeholder="tr('命中', 'Accuracy')"
                clearable
                size="default"
                class="w-full"
                @change="handleSearch"
              >
                <el-option
                  :label="tr('必中', 'Always')"
                  value="100"
                />
                <el-option
                  label="75-99"
                  value="75-99"
                />
                <el-option
                  label="50-74"
                  value="50-74"
                />
                <el-option
                  :label="tr('<50', '<50')"
                  value="0-49"
                />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">PP</label>
              <el-select
                v-model="ppRange"
                :placeholder="tr('PP', 'PP')"
                clearable
                size="default"
                class="w-full"
                @change="handleSearch"
              >
                <el-option
                  label="1-5"
                  value="1-5"
                />
                <el-option
                  label="6-10"
                  value="6-10"
                />
                <el-option
                  label="11-20"
                  value="11-20"
                />
                <el-option
                  :label="tr('21+', '21+')"
                  value="21+"
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
                <el-option
                  :label="tr('威力↑', 'Power ↑')"
                  value="power-asc"
                />
                <el-option
                  :label="tr('威力↓', 'Power ↓')"
                  value="power-desc"
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
        {{ tr('共 {total} 个技能', '{total} moves total', { total }) }}
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
      :count="12"
      :view-mode="viewMode"
    />

    <!-- 网格视图 -->
    <template v-if="viewMode === 'grid' && !loading">
      <div
        v-if="filteredMoves.length"
        class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4"
      >
        <div
          v-for="(move, index) in filteredMoves"
          :key="move.id"
          class="shine-effect glass-card-interactive glass-card p-4 cursor-pointer animate-slide-up relative overflow-hidden group"
          :style="{ animationDelay: `${index * 25}ms` }"
          @click="showMoveDetail(move)"
        >
          <div
            class="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-10"
            :style="{ background: `linear-gradient(135deg, ${move.typeColor || '#888'}20, ${move.typeColor || '#888'}40)` }"
          />
          <div class="relative z-10">
            <div class="flex items-start justify-between mb-2">
              <h3 class="font-semibold text-slate-800 text-sm leading-tight truncate flex-1 group-hover:text-blue-700 transition-colors">
                {{ move.name }}
              </h3>
              <FavoriteButton
                :is-favorited="favorites.includes(move.id)"
                size="sm"
                @toggle="toggleFavorite(move)"
              />
            </div>
            <div class="flex flex-wrap gap-1.5 mb-2">
              <span
                v-if="move.typeName"
                class="type-badge type-badge-sm shadow-md"
                :style="{ backgroundColor: move.typeColor || '#888' }"
              >{{ move.typeName }}</span>
              <span
                class="inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-semibold shadow-sm"
                :class="{
                  'bg-rose-100 text-rose-700': move.damageClass === 'physical',
                  'bg-blue-100 text-blue-700': move.damageClass === 'special',
                  'bg-purple-100 text-purple-700': move.damageClass === 'status'
                }"
              >
                {{ move.damageClass === 'physical' ? tr('物理', 'Phys') : move.damageClass === 'special' ? tr('特殊', 'Spec') : tr('变化', 'Sts') }}
              </span>
            </div>
            <div class="flex items-center gap-3 text-xs text-slate-500">
              <span
                v-if="move.power != null"
                class="font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded"
              >{{ tr('威力', 'Pwr') }} {{ move.power }}</span>
              <span
                v-if="move.accuracy != null"
                class="font-medium"
              >{{ move.accuracy }}%</span>
              <span>PP {{ move.pp }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 列表视图 -->
    <template v-if="viewMode === 'list' && !loading">
      <div
        v-if="filteredMoves.length"
        class="space-y-2"
      >
        <div
          v-for="(move, index) in filteredMoves"
          :key="move.id"
          class="glass-card-interactive glass-card p-3 flex items-center gap-4 cursor-pointer animate-slide-up"
          :style="{ animationDelay: `${index * 15}ms` }"
          @click="showMoveDetail(move)"
        >
          <span class="text-xs text-slate-400 w-10 font-mono">#{{ move.id }}</span>
          <div class="flex-1 flex items-center gap-3">
            <h3 class="font-semibold text-slate-800 text-sm">
              {{ move.name }}
            </h3>
            <span
              v-if="move.typeName"
              class="type-badge type-badge-sm"
              :style="{ backgroundColor: move.typeColor || '#888' }"
            >{{ move.typeName }}</span>
            <span
              class="inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-semibold"
              :class="{
                'bg-rose-100 text-rose-700': move.damageClass === 'physical',
                'bg-blue-100 text-blue-700': move.damageClass === 'special',
                'bg-purple-100 text-purple-700': move.damageClass === 'status'
              }"
            >
              {{ move.damageClass === 'physical' ? tr('物理', 'Phys') : move.damageClass === 'special' ? tr('特殊', 'Spec') : tr('变化', 'Sts') }}
            </span>
          </div>
          <div class="flex items-center gap-4 text-xs text-slate-500">
            <span
              v-if="move.power != null"
              class="font-semibold text-slate-700"
            >{{ move.power }}</span>
            <span>{{ move.accuracy != null ? `${move.accuracy}%` : '-' }}</span>
            <span>PP {{ move.pp }}</span>
          </div>
          <FavoriteButton
            :is-favorited="favorites.includes(move.id)"
            size="lg"
            @toggle="toggleFavorite(move)"
          />
        </div>
      </div>
    </template>

    <!-- 空状态 -->
    <EmptyState
      v-if="!loading && filteredMoves.length === 0"
      :message="tr('没有找到技能', 'No moves found')"
    />

    <!-- 加载更多 -->
    <LoadMoreTrigger
      :loading-more="loadingMore"
      :has-more="hasMore"
      :loaded-count="loadedCount"
      :total="total"
      @load-more="fetchItems(true)"
    />

    <!-- 技能详情弹窗 -->
    <MoveDetailDialog
      v-model:visible="showDetailDialog"
      :move="selectedMove"
    />
  </div>
</template>

<script setup>
/**
 * 技能列表页 / Move List Page
 *
 * 使用 useCatalogList 管理分页、收藏、视图、IntersectionObserver。
 * 保留独特的类型/分类/威力/命中/PP 筛选逻辑。
 * 详情弹窗提取为 MoveDetailDialog 子组件。
 */
import { ref, computed, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { moveApi, typeApi } from '../services/api.js'
import { dataCache } from '../services/cache.js'
import { useLocale } from '../composables/useLocale'
import { useCatalogList } from '../composables/useCatalogList'
import CatalogSkeleton from '../components/CatalogSkeleton.vue'
import LoadMoreTrigger from '../components/LoadMoreTrigger.vue'
import EmptyState from '../components/EmptyState.vue'
import FavoriteButton from '../components/FavoriteButton.vue'
import MoveDetailDialog from '../components/MoveDetailDialog.vue'

const { translate: tr } = useLocale()

// ---- 筛选状态（必须在 useCatalogList 之前定义，否则 TDZ 报错） ----
const selectedType = ref('')
const selectedDamageClass = ref('')
const powerRange = ref('')
const accuracyRange = ref('')
const ppRange = ref('')
const sortBy = ref('default')
const showFilters = ref(false)

// ---- 通用列表逻辑 ----
const {
  items,       // 原始 moves 数据（未筛选）
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
  fetchFn: (params) => moveApi.getList({
    ...params,
    typeId: selectedType.value || undefined
  }),
  favoritesKey: 'move-favorites',
  pageSize: 48
})

// ---- 类型数据 ----
const types = ref([])

dataCache.getOrFetch('types', {}, async () => {
  const result = await typeApi.getAll()
  if (result.code === 200) {
    types.value = result.data
  }
  return result
})

// ---- 客户端筛选 ----
const filteredMoves = ref([])

function applyFilters() {
  let result = [...items.value]

  if (isShowFavorites.value) {
    result = result.filter(m => favorites.value.includes(m.id))
  }
  if (selectedType.value) {
    result = result.filter(m => m.typeId === selectedType.value)
  }
  if (selectedDamageClass.value) {
    result = result.filter(m => m.damageClass === selectedDamageClass.value)
  }
  if (powerRange.value) {
    if (powerRange.value === 'none') {
      result = result.filter(m => m.power === null || m.power === 0)
    } else if (powerRange.value === '151+') {
      result = result.filter(m => m.power >= 151)
    } else {
      const [min, max] = powerRange.value.split('-').map(Number)
      result = result.filter(m => m.power >= min && m.power <= max)
    }
  }
  if (accuracyRange.value) {
    if (accuracyRange.value === '100') {
      result = result.filter(m => m.accuracy === 100 || m.accuracy === null)
    } else {
      const [min, max] = accuracyRange.value.split('-').map(Number)
      result = result.filter(m => m.accuracy >= min && m.accuracy <= max)
    }
  }
  if (ppRange.value) {
    if (ppRange.value === '21+') {
      result = result.filter(m => m.pp >= 21)
    } else {
      const [min, max] = ppRange.value.split('-').map(Number)
      result = result.filter(m => m.pp >= min && m.pp <= max)
    }
  }
  if (sortBy.value !== 'default') {
    const [field, order] = sortBy.value.split('-')
    result.sort((a, b) => {
      const valA = a[field] ?? (field === 'name' ? '' : 0)
      const valB = b[field] ?? (field === 'name' ? '' : 0)
      if (typeof valA === 'string') {
        return order === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA)
      }
      return order === 'asc' ? valA - valB : valB - valA
    })
  }

  // 客户端分页：只展示当前页的数据
  filteredMoves.value = result.slice(0, displayCount.value)
}

// items 或 displayCount 变化时重新筛选（支持懒加载）
watch(() => items.value.length, () => { applyFilters() })
watch(() => displayCount.value, () => { applyFilters() })
watch([selectedType, selectedDamageClass, powerRange, accuracyRange, ppRange, sortBy, isShowFavorites], () => {
  applyFilters()
})

// ---- 详情弹窗 ----
const showDetailDialog = ref(false)
const selectedMove = ref(null)

function showMoveDetail(move) {
  selectedMove.value = move
  showDetailDialog.value = true
}
</script>

<style scoped>
.move-list { padding-bottom: 1rem; }

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
  .move-list { padding-bottom: 0.5rem; }
}
</style>
