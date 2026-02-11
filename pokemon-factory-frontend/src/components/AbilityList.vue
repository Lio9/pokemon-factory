<template>
  <div class="ability-list">
    <h2>特性列表</h2>
    
    <!-- 搜索框 -->
    <div class="search-box">
      <el-input 
        v-model="searchKeyword" 
        placeholder="搜索特性..." 
        style="width: 300px; margin-right: 10px;"
        @keyup.enter="searchAbilities"
      />
      <el-button
        type="primary"
        @click="searchAbilities"
      >
        搜索
      </el-button>
      <el-button @click="resetSearch">
        重置
      </el-button>
    </div>
    
    <!-- 特性列表 -->
    <div 
      ref="scrollContainer"
      style="overflow-y: auto; max-height: 600px;"
      @scroll="handleScroll"
    >
      <el-table
        v-loading="loading && currentPage === 1"
        :data="displayedAbilityList"
        style="width: 100%"
      >
        <el-table-column
          prop="name"
          label="特性名"
        />
        <el-table-column
          prop="generation"
          label="世代"
        />
        <el-table-column
          label="操作"
          width="150"
        >
          <template #default="scope">
            <el-button
              size="mini"
              @click="viewDetails(scope.row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <p
        v-if="loading && currentPage > 1"
        style="text-align: center; padding: 20px;"
      >
        加载中...
      </p>
      <p
        v-if="noMore && displayedAbilityList.length > 0"
        style="text-align: center; padding: 20px;"
      >
        没有更多数据了
      </p>
    
      <!-- 初始加载指示器 -->
      <div
        v-if="loading && currentPage === 1"
        style="text-align: center; padding: 40px;"
      >
        <div style="display: inline-block;">
          <div style="width: 30px; height: 30px; border: 2px solid #eee; border-top-color: #409eff; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto;" />
          <p style="margin-top: 10px; color: #909399;">
            正在加载特性数据...
          </p>
        </div>
      </div>
    
      <!-- 空状态 -->
      <div
        v-if="!loading && noMore && displayedAbilityList.length === 0"
        style="text-align: center; padding: 40px; color: #909399;"
      >
        没有找到相关特性
      </div>
    </div>
    
    <!-- 特性详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="selectedAbility?.name"
      width="50%"
    >
      <div v-if="selectedAbility">
        <el-descriptions
          :column="2"
          border
        >
          <el-descriptions-item label="中文名">
            {{ selectedAbility.name }}
          </el-descriptions-item>
          <el-descriptions-item label="英文名">
            {{ selectedAbility.nameEn }}
          </el-descriptions-item>
          <el-descriptions-item label="日文名">
            {{ selectedAbility.nameJp }}
          </el-descriptions-item>
          <el-descriptions-item label="世代">
            {{ selectedAbility.generation }}
          </el-descriptions-item>
          <el-descriptions-item
            label="描述"
            :span="2"
          >
            {{ selectedAbility.description }}
          </el-descriptions-item>
          <el-descriptions-item
            label="效果"
            :span="2"
          >
            {{ selectedAbility.effect }}
          </el-descriptions-item>
          <el-descriptions-item label="普通特性宝可梦数">
            {{ selectedAbility.commonCount }}
          </el-descriptions-item>
          <el-descriptions-item label="隐藏特性宝可梦数">
            {{ selectedAbility.hiddenCount }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { abilityApi } from '../services/api.js'

export default {
  name: 'AbilityList',
  setup() {
    const abilityList = ref([])
    const displayedAbilityList = ref([])
    const loading = ref(false)
    const searchKeyword = ref('')
    const currentPage = ref(1)
    const pageSize = ref(20)
    const total = ref(0)
    const dialogVisible = ref(false)
    const selectedAbility = ref(null)

    const fetchAbilityList = async () => {
      loading.value = true
      try {
        let result
        if (searchKeyword.value) {
          result = await abilityApi.search(searchKeyword.value, currentPage.value, pageSize.value)
        } else {
          result = await abilityApi.getList({
            current: currentPage.value,
            size: pageSize.value
          })
        }

        if (result.code === 200) {
          abilityList.value = result.data.records || result.data
          displayedAbilityList.value = abilityList.value
          total.value = result.data.total || abilityList.value.length
        } else {
          ElMessage.error(result.message || '获取数据失败')
        }
      } catch (error) {
        console.error('获取特性列表失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      } finally {
        loading.value = false
      }
    }

    const searchAbilities = () => {
      currentPage.value = 1
      fetchAbilityList()
    }

    const resetSearch = () => {
      searchKeyword.value = ''
      currentPage.value = 1
      fetchAbilityList()
    }

    const viewDetails = async (ability) => {
      try {
        const result = await abilityApi.getDetail(ability.id)
        if (result.code === 200) {
          selectedAbility.value = result.data
          dialogVisible.value = true
        } else {
          ElMessage.error('获取详情失败')
        }
      } catch (error) {
        console.error('获取特性详情失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      }
    }

    onMounted(() => {
      fetchAbilityList()
    })

    return {
      abilityList,
      displayedAbilityList,
      loading,
      searchKeyword,
      currentPage,
      pageSize,
      total,
      dialogVisible,
      selectedAbility,
      fetchAbilityList,
      searchAbilities,
      resetSearch,
      viewDetails
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