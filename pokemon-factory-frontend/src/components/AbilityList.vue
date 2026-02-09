<template>
  <div class="ability-list">
    <h2>特性列表</h2>
    
    <!-- 搜索框 -->
    <div class="search-box">
      <el-input 
        v-model="searchKeyword" 
        placeholder="搜索特性..." 
        @keyup.enter="searchAbilities"
        style="width: 300px; margin-right: 10px;"
      />
      <el-button type="primary" @click="searchAbilities">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>
    
    <!-- 特性列表 -->
    <div 
      ref="scrollContainer"
      @scroll="handleScroll"
      style="overflow-y: auto; max-height: 600px;"
    >
      <el-table :data="displayedAbilityList" style="width: 100%" v-loading="loading && currentPage === 1">
        <el-table-column prop="name" label="特性名"></el-table-column>
        <el-table-column prop="generation" label="世代"></el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="mini" @click="viewDetails(scope.row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="loading && currentPage > 1" style="text-align: center; padding: 20px;">加载中...</p>
      <p v-if="noMore && displayedAbilityList.length > 0" style="text-align: center; padding: 20px;">没有更多数据了</p>
    
      <!-- 初始加载指示器 -->
      <div v-if="loading && currentPage === 1" style="text-align: center; padding: 40px;">
        <div style="display: inline-block;">
          <div style="width: 30px; height: 30px; border: 2px solid #eee; border-top-color: #409eff; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto;"></div>
          <p style="margin-top: 10px; color: #909399;">正在加载特性数据...</p>
        </div>
      </div>
    
      <!-- 空状态 -->
      <div v-if="!loading && noMore && displayedAbilityList.length === 0" style="text-align: center; padding: 40px; color: #909399;">
        没有找到相关特性
      </div>
    </div>
    
    <!-- 特性详情对话框 -->
    <el-dialog :title="selectedAbility?.name" v-model="dialogVisible" width="50%">
      <div v-if="selectedAbility">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="中文名">{{ selectedAbility.name }}</el-descriptions-item>
          <el-descriptions-item label="英文名">{{ selectedAbility.nameEn }}</el-descriptions-item>
          <el-descriptions-item label="日文名">{{ selectedAbility.nameJp }}</el-descriptions-item>
          <el-descriptions-item label="世代">{{ selectedAbility.generation }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ selectedAbility.description }}</el-descriptions-item>
          <el-descriptions-item label="效果" :span="2">{{ selectedAbility.effect }}</el-descriptions-item>
          <el-descriptions-item label="普通特性宝可梦数">{{ selectedAbility.commonCount }}</el-descriptions-item>
          <el-descriptions-item label="隐藏特性宝可梦数">{{ selectedAbility.hiddenCount }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'AbilityList',
  data() {
    return {
      abilityList: [], // 存储所有已加载的数据
      displayedAbilityList: [], // 存储当前显示的数据
      loading: false,
      searchKeyword: '',
      currentPage: 1,
      pageSize: 20,
      total: 0,
      dialogVisible: false,
      selectedAbility: null,
      noMore: false,
      isLoading: false,
      debounceTimer: null,
      scrollThreshold: 100
    }
  },
  mounted() {
    this.fetchAbilityList()
  },
  methods: {
    async fetchAbilityList() {
      // 防止重复加载
      if (this.isLoading || this.noMore) {
        return
      }
      
      this.isLoading = true
      this.loading = true
      
      try {
        // 使用POST请求获取列表数据
        const requestData = {
          page: this.currentPage,
          size: this.pageSize,
          keyword: this.searchKeyword || undefined
        }
        
        const response = await axios.post(`/api/abilities/list`, requestData)
        const newData = response.data.records
        
        // 如果是第一页，替换数据；否则追加数据
        if (this.currentPage === 1) {
          this.abilityList = newData
          this.displayedAbilityList = newData
        } else {
          this.abilityList = [...this.abilityList, ...newData]
          // 只显示新数据的一部分以获得更好的性能
          this.displayedAbilityList = this.abilityList.slice(0, this.currentPage * this.pageSize)
        }
        
        this.total = response.data.total
        // 检查是否还有更多数据
        this.noMore = this.abilityList.length >= this.total
        
        // 如果是第一页且没有数据，显示空状态
        if (this.currentPage === 1 && newData.length === 0) {
          this.noMore = true
        }
      } catch (error) {
        console.error('获取特性列表失败:', error)
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
      await this.fetchAbilityList()
    },
    
    async searchAbilities() {
      // 搜索时重置到第一页
      this.currentPage = 1
      this.noMore = false
      await this.fetchAbilityList()
    },
    
    resetSearch() {
      this.searchKeyword = ''
      this.currentPage = 1
      this.noMore = false
      this.fetchAbilityList()
    },
    
    async viewDetails(ability) {
      // 获取特性详情
      try {
        const response = await axios.post(`/api/abilities/detail`, { id: ability.id })
        this.selectedAbility = response.data
        this.dialogVisible = true
      } catch (error) {
        console.error('获取特性详情失败:', error)
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
.ability-list {
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