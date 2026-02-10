<template>
  <div class="move-list">
    <!-- 搜索区域 -->
    <div class="search-section mb-6 bg-gray-50 rounded-xl p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索招式名称..."
            prefix-icon="Search"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <Search class="w-4 h-4" />
              </el-button>
            </template>
          </el-input>
        </div>
        <div class="w-48">
          <el-select
            v-model="selectedCategory"
            placeholder="选择分类"
            clearable
            @change="handleCategoryChange"
          >
            <el-option label="物理" value="物理" />
            <el-option label="特殊" value="特殊" />
            <el-option label="变化" value="变化" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section mb-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-blue-600">{{ totalMoves }}</div>
          <div class="text-gray-600 text-sm">总招式数</div>
        </div>
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-green-600">{{ totalPages }}</div>
          <div class="text-gray-600 text-sm">总页数</div>
        </div>
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-purple-600">{{ currentPage }}</div>
          <div class="text-gray-600 text-sm">当前页</div>
        </div>
      </div>
    </div>

    <!-- 招式列表 -->
    <div v-if="loading" class="text-center py-12">
      <el-skeleton :rows="5" animated />
    </div>
    
    <div v-else-if="moves.length > 0">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div
          v-for="move in moves"
          :key="move.id"
          class="move-card bg-white rounded-xl border border-gray-200 hover:border-green-300 hover:shadow-lg transition-all duration-200 p-4"
        >
          <div class="flex justify-between items-start mb-3">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ move.name }}</h3>
              <div class="flex items-center gap-2 mt-1">
                <span class="px-2 py-1 bg-gray-100 text-gray-700 rounded text-xs">{{ move.type }}</span>
                <span class="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs">{{ move.category }}</span>
              </div>
            </div>
            <div class="text-right">
              <div class="text-sm text-gray-500">#{{
                move.id
              }}</div>
            </div>
          </div>
          <div class="flex justify-between items-center">
            <div>
              <div class="text-sm text-gray-500">威力: {{ move.power }}</div>
              <div class="text-sm text-gray-500">命中: {{ move.accuracy }}</div>
              <div class="text-sm text-gray-500">PP: {{ move.pp }}</div>
            </div>
            <div>
              <el-button type="primary" @click="viewDetails(move)">查看详情</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-12">
      <p class="text-gray-600">没有找到相关招式</p>
    </div>

    <!-- 分页 -->
    <div class="pagination-section mt-6">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="totalMoves"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 招式详情对话框 -->
    <el-dialog :title="selectedMove?.name" v-model="dialogVisible" width="50%">
      <div v-if="selectedMove">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="中文名">{{ selectedMove.name }}</el-descriptions-item>
          <el-descriptions-item label="英文名">{{ selectedMove.nameEn }}</el-descriptions-item>
          <el-descriptions-item label="日文名">{{ selectedMove.nameJp }}</el-descriptions-item>
          <el-descriptions-item label="属性">{{ selectedMove.type }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ selectedMove.category }}</el-descriptions-item>
          <el-descriptions-item label="威力">{{ selectedMove.power }}</el-descriptions-item>
          <el-descriptions-item label="命中">{{ selectedMove.accuracy }}</el-descriptions-item>
          <el-descriptions-item label="PP">{{ selectedMove.pp }}</el-descriptions-item>
          <el-descriptions-item label="优先度">{{ selectedMove.priority }}</el-descriptions-item>
          <el-descriptions-item label="击中要害概率">{{ selectedMove.critRate }}</el-descriptions-item>
          <el-descriptions-item label="作用目标">{{ selectedMove.target }}</el-descriptions-item>
          <el-descriptions-item label="华丽大赛属性">{{ selectedMove.contestType }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ selectedMove.description }}</el-descriptions-item>
          <el-descriptions-item label="效果" :span="2">{{ selectedMove.effect }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'MoveList',
  data() {
    return {
      moves: [], // 存储所有已加载的数据
      loading: false,
      searchKeyword: '',
      selectedCategory: '',
      currentPage: 1,
      pageSize: 20,
      totalMoves: 0,
      totalPages: 0,
      dialogVisible: false,
      selectedMove: null,
      noMore: false,
      isLoading: false,
      debounceTimer: null,
      scrollThreshold: 100,
      // 示例数据
      sampleMoves: [
        {
          id: 1,
          name: '撞击',
          nameEn: 'tackle',
          type: '一般',
          category: '物理',
          power: '40',
          accuracy: '100',
          pp: '35',
          description: '用身体撞向对手进行攻击。'
        },
        {
          id: 2,
          name: '藤鞭',
          nameEn: 'vine whip',
          type: '草',
          category: '物理',
          power: '45',
          accuracy: '100',
          pp: '25',
          description: '用细长的藤蔓抽打对手。'
        },
        {
          id: 3,
          name: '毒粉',
          nameEn: 'poison powder',
          type: '毒',
          category: '变化',
          power: '-',
          accuracy: '75',
          pp: '35',
          description: '撒出毒粉，让对手陷入中毒状态。'
        },
        {
          id: 4,
          name: '寄生种子',
          nameEn: 'leech seed',
          type: '草',
          category: '变化',
          power: '-',
          accuracy: '90',
          pp: '10',
          description: '植入寄生种子，每回合吸取对手的ＨＰ。'
        },
        {
          id: 5,
          name: '水枪',
          nameEn: 'water gun',
          type: '水',
          category: '特殊',
          power: '40',
          accuracy: '100',
          pp: '25',
          description: '喷射水流攻击对手。'
        }
      ]
    }
  },
  mounted() {
    this.fetchMoveList()
  },
  methods: {
    async fetchMoveList() {
      // 防止重复加载
      if (this.isLoading || this.noMore) {
        return
      }
      
      this.isLoading = true
      this.loading = true
      
      try {
        // 模拟API调用，使用示例数据
        await new Promise(resolve => setTimeout(resolve, 500))
        
        let newData = []
        if (this.currentPage === 1) {
          // 第一页使用示例数据
          newData = this.sampleMoves
        } else {
          // 后续页面可以添加更多数据
          newData = this.sampleMoves.slice(0, this.pageSize)
        }
        
        // 如果是第一页，替换数据；否则追加数据
        if (this.currentPage === 1) {
          this.moves = newData
        } else {
          this.moves = [...this.moves, ...newData]
        }
        
        this.totalMoves = this.sampleMoves.length
        this.totalPages = Math.ceil(this.totalMoves / this.pageSize)
        // 检查是否还有更多数据
        this.noMore = this.moves.length >= this.totalMoves
        
        // 如果是第一页且没有数据，显示空状态
        if (this.currentPage === 1 && newData.length === 0) {
          this.noMore = true
        }
      } catch (error) {
        console.error('获取招式列表失败:', error)
        this.noMore = true // 出错时停止加载更多
      } finally {
        this.loading = false
        this.isLoading = false
      }
    },
    
    // 处理滚动事件，添加防抖
    handleScroll() {
      clearTimeout(this.debounceTimer)
      this.debounceTimer = setTimeout(() => {
        this.checkScroll()
      }, 100) // 100ms 防抖
    },
    
    // 检查是否需要加载更多
    checkScroll() {
      const container = this.$refs.scrollContainer
      if (!container) return
      
      const { scrollTop, scrollHeight, clientHeight } = container
      // 当距离底部小于阈值时加载更多
      if (scrollTop + clientHeight >= scrollHeight - this.scrollThreshold) {
        this.loadMore()
      }
    },
    
    async loadMore() {
      if (this.noMore || this.isLoading) return
      this.currentPage++
      await this.fetchMoveList()
    },
    
    async handleSearch() {
      // 搜索时重置到第一页
      this.currentPage = 1
      this.noMore = false
      await this.fetchMoveList()
    },
    
    handleCategoryChange() {
      this.currentPage = 1
      this.noMore = false
      this.fetchMoveList()
    },
    
    handlePageChange(page) {
      this.currentPage = page
      this.fetchMoveList()
    },
    
    async viewDetails(move) {
      // 获取招式详情
      try {
        // 模拟API调用，使用示例数据
        await new Promise(resolve => setTimeout(resolve, 300))
        
        // 在示例数据中查找匹配的技能
        const matchedMove = this.sampleMoves.find(m => m.id === move.id)
        if (matchedMove) {
          this.selectedMove = matchedMove
        } else {
          // 如果没有找到，创建一个基础的详情对象
          this.selectedMove = {
            ...move,
            nameEn: move.nameEn || 'unknown',
            type: move.type || '一般',
            category: move.category || '变化',
            power: move.power || '-',
            accuracy: move.accuracy || '-',
            pp: move.pp || '-',
            description: move.description || '暂无描述'
          }
        }
        
        this.dialogVisible = true
      } catch (error) {
        console.error('获取招式详情失败:', error)
      }
    }
  },
  beforeUnmount() {
    // 清除定时器
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer)
    }
  }
}
</script>

<style scoped>
.move-list {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.search-box {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>