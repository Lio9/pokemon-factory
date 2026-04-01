<template>
  <div v-if="pokemon" class="pokemon-detail">
    <!-- 面包屑导航 -->
    <nav class="mb-6 flex items-center gap-2 text-sm">
      <router-link to="/" class="text-gray-500 hover:text-blue-600 transition-colors">首页</router-link>
      <ChevronRight class="w-4 h-4 text-gray-400" />
      <router-link to="/pokemon" class="text-gray-500 hover:text-blue-600 transition-colors">图鉴</router-link>
      <ChevronRight class="w-4 h-4 text-gray-400" />
      <span class="text-gray-900 font-medium">{{ pokemon.name }}</span>
    </nav>

    <!-- 快捷操作栏 -->
    <div class="fixed top-4 right-4 z-50 flex gap-2">
      <button 
        @click="toggleFavorite"
        class="w-12 h-12 rounded-full flex items-center justify-center shadow-lg transition-all duration-300 hover:scale-110"
        :class="isFavorite ? 'bg-gradient-to-r from-pink-500 to-rose-500 text-white' : 'bg-white text-gray-600 hover:text-pink-500'"
        title="收藏"
      >
        <Heart :class="['w-6 h-6 transition-all duration-300', isFavorite ? 'fill-current scale-110' : '']" />
      </button>
      <button 
        @click="showCompareModal = true"
        class="w-12 h-12 rounded-full bg-white text-gray-600 hover:text-blue-600 shadow-lg transition-all duration-300 hover:scale-110 flex items-center justify-center"
        title="比较"
      >
        <Scale class="w-6 h-6" />
      </button>
      <button 
        @click="sharePokemon"
        class="w-12 h-12 rounded-full bg-white text-gray-600 hover:text-green-600 shadow-lg transition-all duration-300 hover:scale-110 flex items-center justify-center"
        title="分享"
      >
        <Share2 class="w-6 h-6" />
      </button>
    </div>

    <!-- 比较模态框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showCompareModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" @click="showCompareModal = false"></div>
          <div class="relative bg-white rounded-3xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
            <button 
              @click="showCompareModal = false"
              class="absolute top-4 right-4 w-10 h-10 rounded-full bg-gray-100 hover:bg-gray-200 flex items-center justify-center transition-colors"
            >
              <X class="w-5 h-5 text-gray-600" />
            </button>
            <div class="p-6">
              <h2 class="text-2xl font-bold text-gray-900 mb-6">宝可梦比较</h2>
              <CompareView :pokemon1="pokemon" :pokemon2="comparePokemon" @select-compare="selectComparePokemon" />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 基本信息卡片 -->
    <div class="bg-white rounded-3xl shadow-xl overflow-hidden mb-6 border border-gray-100">
      <div class="md:flex">
        <!-- 图片区域 -->
        <div class="md:w-1/3 bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 p-8 flex flex-col items-center justify-center relative">
          <!-- 图片切换按钮 -->
          <div class="absolute top-4 right-4 flex gap-2">
            <button 
              v-for="(label, type) in imageTypes" 
              :key="type"
              @click="imageMode = type"
              class="w-10 h-10 rounded-full flex items-center justify-center transition-all"
              :class="imageMode === type ? 'bg-blue-600 text-white shadow-lg scale-110' : 'bg-white/70 text-gray-600 hover:bg-white'"
              :title="label"
            >
              <component :is="getImageIcon(type)" class="w-5 h-5" />
            </button>
          </div>

          <div class="relative">
            <!-- 懒加载占位 -->
            <div 
              v-if="!imageLoaded" 
              class="w-72 h-72 flex items-center justify-center"
            >
              <div class="w-40 h-40 rounded-full bg-gradient-to-br from-blue-200 to-indigo-200 animate-pulse flex items-center justify-center">
                <Loader2 class="w-12 h-12 text-blue-400 animate-spin" />
              </div>
            </div>
            <img 
              v-show="imageLoaded"
              :src="currentImageUrl"
              :alt="pokemon.name"
              class="w-72 h-72 object-contain drop-shadow-2xl transition-transform duration-300 hover:scale-105"
              @load="imageLoaded = true"
              @error="handleImageError"
            >
            <!-- 图鉴编号 -->
            <div class="absolute -top-3 -left-3 bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-lg font-bold px-5 py-2 rounded-2xl shadow-lg flex items-center gap-2">
              <Hash class="w-4 h-4" />
              {{ String(pokemon.id).padStart(4, '0') }}
            </div>
            <!-- 形态标记 -->
            <div class="absolute -top-3 -right-3 flex gap-1">
              <span v-if="currentForm?.isMega" class="px-3 py-1 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-xs font-bold rounded-full shadow-lg">
                MEGA
              </span>
              <span v-if="currentForm?.isGigantamax" class="px-3 py-1 bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs font-bold rounded-full shadow-lg">
                极巨化
              </span>
            </div>
          </div>

          <!-- 图片类型说明 -->
          <p class="mt-4 text-sm text-gray-500 font-medium">{{ imageTypes[imageMode] }}</p>
        </div>
        
        <!-- 信息区域 -->
        <div class="md:w-2/3 p-8">
          <div class="flex items-start justify-between mb-6">
            <div>
              <h1 class="text-4xl font-bold bg-gradient-to-r from-gray-900 via-gray-800 to-gray-700 bg-clip-text text-transparent">
                {{ pokemon.name }}
              </h1>
              <p class="text-gray-500 mt-1 text-lg">{{ pokemon.nameEn }} / {{ pokemon.nameJp }}</p>
              <p class="text-blue-600 font-medium mt-2">{{ pokemon.genus }}</p>
            </div>
            <div class="flex gap-2">
              <span v-if="pokemon.isLegendary" class="px-4 py-2 bg-gradient-to-r from-amber-100 to-yellow-100 text-amber-800 rounded-full text-sm font-bold shadow-sm flex items-center gap-1">
                <Star class="w-4 h-4" />
                传说
              </span>
              <span v-if="pokemon.isMythical" class="px-4 py-2 bg-gradient-to-r from-purple-100 to-pink-100 text-purple-800 rounded-full text-sm font-bold shadow-sm flex items-center gap-1">
                <Sparkles class="w-4 h-4" />
                神话
              </span>
              <span v-if="pokemon.isBaby" class="px-4 py-2 bg-gradient-to-r from-pink-100 to-rose-100 text-pink-800 rounded-full text-sm font-bold shadow-sm flex items-center gap-1">
                <Baby class="w-4 h-4" />
                幼崽
              </span>
            </div>
          </div>
          
          <!-- 属性 -->
          <div class="mb-6">
            <span class="text-gray-500 text-sm font-medium">属性</span>
            <div class="inline-flex gap-2 ml-2">
              <span 
                v-for="type in currentForm?.types || []"
                :key="type.id"
                class="px-4 py-2 rounded-xl text-white text-sm font-bold shadow-md hover:shadow-lg transition-shadow"
                :style="{ backgroundColor: type.color }"
              >
                {{ type.name }}
              </span>
            </div>
          </div>
          
          <!-- 基本信息 -->
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div class="bg-gradient-to-br from-gray-50 to-slate-50 rounded-2xl p-4 text-center border border-gray-100 hover:shadow-md transition-shadow">
              <div class="text-gray-500 text-xs font-medium mb-1">身高</div>
              <div class="text-xl font-bold text-gray-900">{{ currentForm?.height || '-' }}m</div>
            </div>
            <div class="bg-gradient-to-br from-gray-50 to-slate-50 rounded-2xl p-4 text-center border border-gray-100 hover:shadow-md transition-shadow">
              <div class="text-gray-500 text-xs font-medium mb-1">体重</div>
              <div class="text-xl font-bold text-gray-900">{{ currentForm?.weight || '-' }}kg</div>
            </div>
            <div class="bg-gradient-to-br from-gray-50 to-slate-50 rounded-2xl p-4 text-center border border-gray-100 hover:shadow-md transition-shadow">
              <div class="text-gray-500 text-xs font-medium mb-1">捕获率</div>
              <div class="text-xl font-bold text-gray-900">{{ pokemon.captureRate || '-' }}</div>
            </div>
            <div class="bg-gradient-to-br from-gray-50 to-slate-50 rounded-2xl p-4 text-center border border-gray-100 hover:shadow-md transition-shadow">
              <div class="text-gray-500 text-xs font-medium mb-1">亲密度</div>
              <div class="text-xl font-bold text-gray-900">{{ pokemon.baseHappiness || '-' }}</div>
            </div>
          </div>

          <!-- 补充信息 -->
          <div class="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
            <div class="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-3 border border-blue-100">
              <div class="text-blue-600 text-xs font-medium mb-1">性别比例</div>
              <div class="text-base font-semibold text-gray-900">{{ getGenderRatioText(pokemon.genderRate) }}</div>
            </div>
            <div class="bg-gradient-to-br from-purple-50 to-pink-50 rounded-xl p-3 border border-purple-100">
              <div class="text-purple-600 text-xs font-medium mb-1">蛋群</div>
              <div class="text-base font-semibold text-gray-900">{{ (pokemon.eggGroups || []).join(' / ') || '-' }}</div>
            </div>
            <div class="bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl p-3 border border-green-100">
              <div class="text-green-600 text-xs font-medium mb-1">孵化步数</div>
              <div class="text-base font-semibold text-gray-900">{{ pokemon.hatchCounter ? `${pokemon.hatchCounter * 255}` : '-' }}</div>
            </div>
            <div class="bg-gradient-to-br from-amber-50 to-orange-50 rounded-xl p-3 border border-amber-100">
              <div class="text-amber-600 text-xs font-medium mb-1">成长类型</div>
              <div class="text-base font-semibold text-gray-900">{{ pokemon.growthRate || '-' }}</div>
            </div>
            <div class="bg-gradient-to-br from-rose-50 to-red-50 rounded-xl p-3 border border-rose-100">
              <div class="text-rose-600 text-xs font-medium mb-1">基础经验</div>
              <div class="text-base font-semibold text-gray-900">{{ currentForm?.baseExperience || '-' }}</div>
            </div>
            <div class="bg-gradient-to-br from-cyan-50 to-sky-50 rounded-xl p-3 border border-cyan-100">
              <div class="text-cyan-600 text-xs font-medium mb-1">世代</div>
              <div class="text-base font-semibold text-gray-900">第 {{ pokemon.generationId }} 世代</div>
            </div>
          </div>
          
          <!-- 描述 -->
          <div class="bg-gradient-to-r from-slate-50 to-gray-50 rounded-2xl p-5 border border-gray-100">
            <p class="text-gray-700 leading-relaxed">{{ pokemon.description || '暂无描述' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 种族值 -->
    <div v-if="currentForm?.stats" class="bg-white rounded-3xl shadow-xl p-8 mb-6 border border-gray-100">
      <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-3">
        <div class="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center">
          <BarChart3 class="w-5 h-5 text-white" />
        </div>
        种族值
        <button 
          @click="statsViewMode = statsViewMode === 'bar' ? 'radar' : 'bar'"
          class="ml-auto px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-xl text-sm font-medium transition-colors"
        >
          {{ statsViewMode === 'bar' ? '雷达图' : '进度条' }}
        </button>
      </h2>
      
      <!-- 进度条视图 -->
      <div v-if="statsViewMode === 'bar'" class="space-y-4">
        <StatBar label="HP" :value="currentForm.stats.hp" color="#FF6B6B" />
        <StatBar label="攻击" :value="currentForm.stats.attack" color="#FFA94D" />
        <StatBar label="防御" :value="currentForm.stats.defense" color="#FFD43B" />
        <StatBar label="特攻" :value="currentForm.stats.spAttack" color="#4DABF7" />
        <StatBar label="特防" :value="currentForm.stats.spDefense" color="#69DB7C" />
        <StatBar label="速度" :value="currentForm.stats.speed" color="#F783AC" />
      </div>
      
      <!-- 雷达图视图 -->
      <div v-else class="flex justify-center py-4">
        <RadarChart :stats="currentForm.stats" />
      </div>
      
      <div class="mt-8 text-center bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl p-4 border border-blue-100">
        <span class="text-4xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">{{ currentForm.stats.total }}</span>
        <span class="text-gray-600 ml-3 text-lg">种族值总和</span>
      </div>
    </div>

    <!-- 特性 -->
    <div v-if="currentForm?.abilities?.length" class="bg-white rounded-3xl shadow-xl p-8 mb-6 border border-gray-100">
      <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-3">
        <div class="w-10 h-10 bg-gradient-to-br from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
          <Sparkles class="w-5 h-5 text-white" />
        </div>
        特性
      </h2>
      <div class="grid md:grid-cols-2 gap-4">
        <div 
          v-for="ability in currentForm.abilities" 
          :key="ability.id"
          class="rounded-2xl p-5 hover:shadow-xl transition-all duration-300 cursor-default group"
          :class="ability.isHidden ? 'bg-gradient-to-br from-purple-50 to-pink-50 border-2 border-purple-200' : 'bg-gradient-to-br from-gray-50 to-slate-50 border-2 border-gray-100 hover:border-green-200'"
        >
          <div class="flex items-center justify-between mb-3">
            <span class="font-bold text-lg text-gray-900 group-hover:text-green-600 transition-colors">{{ ability.name }}</span>
            <span v-if="ability.isHidden" class="px-3 py-1 bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs font-bold rounded-full shadow-sm">
              隐藏
            </span>
          </div>
          <p class="text-gray-600 leading-relaxed">{{ ability.description || '暂无描述' }}</p>
        </div>
      </div>
    </div>

    <!-- 形态 -->
    <div v-if="pokemon.forms?.length > 1" class="bg-white rounded-3xl shadow-xl p-8 mb-6 border border-gray-100">
      <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-3">
        <div class="w-10 h-10 bg-gradient-to-br from-orange-500 to-amber-600 rounded-xl flex items-center justify-center">
          <Layers class="w-5 h-5 text-white" />
        </div>
        形态
      </h2>
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
        <div 
          v-for="form in pokemon.forms" 
          :key="form.id"
          class="rounded-2xl p-4 text-center cursor-pointer transition-all duration-300 group"
          :class="selectedFormId === form.id ? 'bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-xl scale-105' : 'bg-gradient-to-br from-gray-50 to-slate-50 border-2 border-gray-200 hover:border-blue-300 hover:shadow-lg'"
          @click="selectedFormId = form.id"
        >
          <div class="relative">
            <img 
              :src="form.spriteUrl || getPokemonImage(pokemon.id)"
              :alt="form.formName"
              class="w-20 h-20 mx-auto object-contain"
              loading="lazy"
            >
            <!-- 形态标记 -->
            <div class="absolute -top-2 -right-2 flex gap-1">
              <span v-if="form.isMega" class="w-5 h-5 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-xs font-bold rounded-full flex items-center justify-center">M</span>
              <span v-if="form.isGigantamax" class="w-5 h-5 bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs font-bold rounded-full flex items-center justify-center">G</span>
            </div>
          </div>
          <p class="mt-3 font-medium" :class="selectedFormId === form.id ? 'text-white' : 'text-gray-900'">{{ form.formName || '默认形态' }}</p>
        </div>
      </div>
    </div>

    <!-- 进化链 -->
    <div v-if="pokemon.evolutionChain?.length" class="bg-white rounded-3xl shadow-xl p-8 mb-6 border border-gray-100">
      <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-3">
        <div class="w-10 h-10 bg-gradient-to-br from-purple-500 to-indigo-600 rounded-xl flex items-center justify-center">
          <GitBranch class="w-5 h-5 text-white" />
        </div>
        进化链
      </h2>
      <div class="flex items-center justify-center flex-wrap gap-4">
        <template v-for="(evo, index) in pokemon.evolutionChain" :key="evo.speciesId">
          <router-link 
            :to="`/pokemon/${evo.speciesId}`"
            class="flex flex-col items-center p-6 rounded-2xl transition-all duration-300 group"
            :class="evo.isCurrent ? 'bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-xl ring-4 ring-blue-300 scale-105' : 'bg-gradient-to-br from-gray-50 to-slate-50 border-2 border-gray-200 hover:border-purple-300 hover:shadow-xl'"
          >
            <div class="relative">
              <img 
                :src="evo.spriteUrl || getPokemonImage(evo.speciesId)"
                :alt="evo.name"
                class="w-28 h-28 object-contain transition-transform duration-300 group-hover:scale-110"
                loading="lazy"
                @error="handleImageError"
              >
              <span v-if="evo.isCurrent" class="absolute -top-2 -left-2 w-6 h-6 bg-white text-blue-600 rounded-full flex items-center justify-center shadow-lg text-xs font-bold">
                ✓
              </span>
            </div>
            <span class="mt-3 font-bold" :class="evo.isCurrent ? 'text-white' : 'text-gray-900'">{{ evo.name }}</span>
            <span class="text-xs mt-1" :class="evo.isCurrent ? 'text-blue-100' : 'text-gray-500'">#{{ String(evo.speciesId).padStart(4, '0') }}</span>
          </router-link>
          <div v-if="index < pokemon.evolutionChain.length - 1" class="flex flex-col items-center text-gray-400">
            <ArrowRight class="w-8 h-8 transition-transform duration-300 group-hover:translate-x-1" />
            <span class="text-xs mt-1 px-3 py-1 bg-gray-100 rounded-full">{{ evo.trigger }}</span>
            <span v-if="evo.minLevel" class="text-xs mt-1 px-3 py-1 bg-blue-100 text-blue-700 rounded-full font-medium">Lv.{{ evo.minLevel }}</span>
          </div>
        </template>
      </div>
    </div>

    <!-- 技能列表 -->
    <div class="bg-white rounded-3xl shadow-xl p-8 border border-gray-100">
      <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-3">
        <div class="w-10 h-10 bg-gradient-to-br from-red-500 to-orange-600 rounded-xl flex items-center justify-center">
          <Zap class="w-5 h-5 text-white" />
        </div>
        可学技能
        <span class="text-lg font-normal text-gray-500 ml-2">({{ moves.length }} 个)</span>
      </h2>

      <!-- 筛选器 -->
      <div class="flex flex-wrap gap-3 mb-6">
        <button
          v-for="filter in moveFilters"
          :key="filter.key"
          @click="selectedMoveFilter = filter.key"
          class="px-4 py-2 rounded-xl font-medium transition-all duration-300"
          :class="selectedMoveFilter === filter.key 
            ? 'bg-gradient-to-r from-blue-500 to-indigo-600 text-white shadow-lg scale-105' 
            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        >
          {{ filter.label }}
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="loadingMoves" class="text-center py-12">
        <el-skeleton :rows="5" animated />
      </div>
      
      <!-- 技能表格 -->
      <div v-else-if="filteredMoves.length" class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b-2 border-gray-100">
              <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">技能</th>
              <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">属性</th>
              <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">分类</th>
              <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">威力</th>
              <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">命中</th>
              <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">PP</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="move in filteredMoves" :key="move.id" class="hover:bg-gradient-to-r from-gray-50 to-blue-50 transition-colors">
              <td class="py-4 px-4">
                <div class="font-bold text-gray-900">{{ move.name }}</div>
                <div class="text-xs text-gray-500 mt-1">
                  <span class="px-2 py-0.5 bg-blue-100 text-blue-700 rounded-full text-xs font-medium">{{ move.learnMethod }}</span>
                  <span v-if="move.level" class="ml-2 px-2 py-0.5 bg-green-100 text-green-700 rounded-full text-xs font-medium">Lv.{{ move.level }}</span>
                </div>
              </td>
              <td class="py-4 px-4">
                <span 
                  class="px-3 py-1.5 rounded-lg text-white text-sm font-bold shadow-sm"
                  :style="{ backgroundColor: move.typeColor }"
                >
                  {{ move.typeName }}
                </span>
              </td>
              <td class="py-4 px-4">
                <span class="px-3 py-1.5 rounded-lg text-sm font-medium" :class="getDamageClassColor(move.damageClass)">
                  {{ move.damageClass }}
                </span>
              </td>
              <td class="py-4 px-4">
                <span class="text-base font-bold" :class="move.power >= 80 ? 'text-red-600' : move.power >= 40 ? 'text-orange-600' : 'text-gray-700'">
                  {{ move.power || '-' }}
                </span>
              </td>
              <td class="py-4 px-4 text-base font-medium text-gray-700">{{ move.accuracy || '-' }}</td>
              <td class="py-4 px-4 text-base font-medium text-gray-700">{{ move.pp || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div v-else class="text-center py-12 text-gray-500 bg-gradient-to-r from-gray-50 to-slate-50 rounded-2xl border-2 border-dashed border-gray-200">
        暂无技能数据
      </div>
    </div>
  </div>
  
  <!-- 加载中骨架屏 -->
  <div v-else class="pokemon-detail">
    <!-- 面包屑骨架 -->
    <div class="mb-6 flex items-center gap-2">
      <el-skeleton-item variant="text" style="width: 60px" />
      <el-skeleton-item variant="text" style="width: 16px" />
      <el-skeleton-item variant="text" style="width: 60px" />
      <el-skeleton-item variant="text" style="width: 16px" />
      <el-skeleton-item variant="text" style="width: 80px" />
    </div>
    
    <!-- 主卡片骨架 -->
    <div class="bg-white rounded-3xl shadow-xl overflow-hidden mb-6 border border-gray-100">
      <div class="md:flex">
        <!-- 图片区域骨架 -->
        <div class="md:w-1/3 bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 p-8 flex items-center justify-center">
          <el-skeleton-item variant="circle" style="width: 220px; height: 220px" />
        </div>
        
        <!-- 信息区域骨架 -->
        <div class="md:w-2/3 p-8">
          <el-skeleton :rows="8" animated />
        </div>
      </div>
    </div>
    
    <!-- 种族值骨架 -->
    <div class="bg-white rounded-3xl shadow-xl p-8 mb-6 border border-gray-100">
      <el-skeleton :rows="10" animated />
    </div>
    
    <!-- 特性骨架 -->
    <div class="bg-white rounded-3xl shadow-xl p-8 mb-6 border border-gray-100">
      <el-skeleton :rows="6" animated />
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChevronLeft, ChevronRight, BarChart3, Sparkles, Layers, GitBranch, ArrowRight, Zap, Hash, Star, Baby, Loader2, Image, RotateCcw, Sparkles as Shiny, Heart, Scale, Share2, X } from 'lucide-vue-next'
import { pokemonApi, sprites } from '../services/api.js'
import { dataCache } from '../services/cache.js'

// 种族值条组件
const StatBar = {
  props: ['label', 'value', 'color'],
  template: `
    <div class="flex items-center gap-4">
      <span class="w-14 text-sm font-bold text-gray-700">{{ label }}</span>
      <div class="flex-1 bg-gray-200 rounded-full h-4 overflow-hidden shadow-inner">
        <div 
          class="h-full rounded-full transition-all duration-700 ease-out relative"
          :style="{ width: (value / 255 * 100) + '%', backgroundColor: color }"
        >
          <div class="absolute inset-0 bg-gradient-to-r from-white/30 to-transparent"></div>
        </div>
      </div>
      <span class="w-12 text-sm font-bold text-right text-gray-900">{{ value || 0 }}</span>
    </div>
  `
}

// 雷达图组件
const RadarChart = {
  props: ['stats'],
  template: `
    <svg viewBox="0 0 300 300" class="w-full max-w-md">
      <!-- 背景网格 -->
      <polygon 
        v-for="level in 5"
        :points="getPolygonPoints(level / 5 * 200)"
        fill="none"
        stroke="#E5E7EB"
        stroke-width="1"
      />
      <!-- 轴线 -->
      <line 
        v-for="(label, i) in ['HP', '攻击', '防御', '特攻', '特防', '速度']"
        :x1="150"
        :y1="150"
        :x2="150 + (i % 2 === 0 ? 1 : -1) * 100 * Math.cos(Math.PI * i / 3)"
        :y2="150 - 100 * Math.sin(Math.PI * i / 3)"
        stroke="#E5E7EB"
        stroke-width="1"
      />
      <!-- 数据区域 -->
      <polygon 
        :points="getDataPoints()"
        fill="rgba(59, 130, 246, 0.3)"
        stroke="#3B82F6"
        stroke-width="2"
      />
      <!-- 数据点 -->
      <circle 
        v-for="(label, i) in ['HP', '攻击', '防御', '特攻', '特防', '速度']"
        :cx="150 + (i % 2 === 0 ? 1 : -1) * getStatValue(i) / 255 * 100 * Math.cos(Math.PI * i / 3)"
        :cy="150 - getStatValue(i) / 255 * 100 * Math.sin(Math.PI * i / 3)"
        r="5"
        fill="#3B82F6"
      />
      <!-- 标签 -->
      <text 
        v-for="(label, i) in ['HP', '攻击', '防御', '特攻', '特防', '速度']"
        :x="150 + (i % 2 === 0 ? 1 : -1) * 120 * Math.cos(Math.PI * i / 3)"
        :y="150 - 120 * Math.sin(Math.PI * i / 3)"
        text-anchor="middle"
        dominant-baseline="middle"
        class="text-xs font-bold fill-gray-600"
      >
        {{ label }}
      </text>
    </svg>
  `,
  methods: {
    getPolygonPoints(radius) {
      const points = []
      for (let i = 0; i < 6; i++) {
        const x = 150 + (i % 2 === 0 ? 1 : -1) * radius * Math.cos(Math.PI * i / 3)
        const y = 150 - radius * Math.sin(Math.PI * i / 3)
        points.push(`${x},${y}`)
      }
      return points.join(' ')
    },
    getStatValue(index) {
      const values = [
        this.stats?.hp || 0,
        this.stats?.attack || 0,
        this.stats?.defense || 0,
        this.stats?.spAttack || 0,
        this.stats?.spDefense || 0,
        this.stats?.speed || 0
      ]
      return values[index] || 0
    },
    getDataPoints() {
      const points = []
      for (let i = 0; i < 6; i++) {
        const value = this.getStatValue(i)
        const x = 150 + (i % 2 === 0 ? 1 : -1) * value / 255 * 100 * Math.cos(Math.PI * i / 3)
        const y = 150 - value / 255 * 100 * Math.sin(Math.PI * i / 3)
        points.push(`${x},${y}`)
      }
      return points.join(' ')
    }
  }
}

// 比较视图组件
const CompareView = {
  props: ['pokemon1', 'pokemon2'],
  emits: ['select-compare'],
  template: `
    <div class="grid md:grid-cols-2 gap-8">
      <div class="text-center">
        <img :src="pokemon1.spriteUrl || getSprite(pokemon1.id)" class="w-32 h-32 mx-auto mb-4">
        <h3 class="font-bold text-lg">{{ pokemon1.name }}</h3>
      </div>
      <div v-if="pokemon2" class="text-center">
        <img :src="pokemon2.spriteUrl || getSprite(pokemon2.id)" class="w-32 h-32 mx-auto mb-4">
        <h3 class="font-bold text-lg">{{ pokemon2.name }}</h3>
      </div>
      <div v-else class="flex items-center justify-center border-2 border-dashed border-gray-200 rounded-2xl">
        <button 
          @click="$emit('select-compare')"
          class="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-medium transition-colors"
        >
          选择宝可梦
        </button>
      </div>
    </div>
  `,
  methods: {
    getSprite(id) {
      return sprites.pokemon(id)
    }
  }
}

export default {
  name: 'PokemonDetail',
  components: { ChevronLeft, ChevronRight, BarChart3, Sparkles, Layers, GitBranch, ArrowRight, Zap, Hash, Star, Baby, Loader2, Image, RotateCcw, Shiny, StatBar, RadarChart, CompareView, Heart, Scale, Share2, X },
  setup() {
    const route = useRoute()
    const router = useRouter()
    
    const pokemon = ref(null)
    const moves = ref([])
    const loadingMoves = ref(false)
    const selectedFormId = ref(null)
    const imageLoaded = ref(false)
    const imageMode = ref('front')
    const selectedMoveFilter = ref('all')
    
    // 新增状态
    const isFavorite = ref(false)
    const showCompareModal = ref(false)
    const comparePokemon = ref(null)
    const statsViewMode = ref('bar')
    const favorites = ref([])
    
    // 加载收藏列表
    const loadFavorites = () => {
      try {
        const stored = localStorage.getItem('pokemon-favorites')
        if (stored) {
          favorites.value = JSON.parse(stored)
        }
      } catch (error) {
        console.error('加载收藏失败:', error)
      }
    }
    
    // 检查是否已收藏
    const checkFavorite = () => {
      isFavorite.value = favorites.value.includes(pokemon.value?.id)
    }
    
    // 切换收藏状态
    const toggleFavorite = () => {
      if (!pokemon.value) return
      
      const index = favorites.value.indexOf(pokemon.value.id)
      if (index > -1) {
        favorites.value.splice(index, 1)
        ElMessage.success('已取消收藏')
      } else {
        favorites.value.push(pokemon.value.id)
        ElMessage.success('已添加收藏')
      }
      
      try {
        localStorage.setItem('pokemon-favorites', JSON.stringify(favorites.value))
      } catch (error) {
        console.error('保存收藏失败:', error)
      }
      
      isFavorite.value = !isFavorite.value
    }
    
    // 分享功能
    const sharePokemon = async () => {
      if (!pokemon.value) return
      
      const url = window.location.href
      
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(url)
          ElMessage.success('链接已复制到剪贴板')
        } else {
          // 降级方案
          const textarea = document.createElement('textarea')
          textarea.value = url
          textarea.style.position = 'fixed'
          textarea.style.opacity = '0'
          document.body.appendChild(textarea)
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
          ElMessage.success('链接已复制到剪贴板')
        }
      } catch (error) {
        console.error('复制失败:', error)
        ElMessage.error('复制失败，请手动复制链接')
      }
    }
    
    // 选择比较宝可梦
    const selectComparePokemon = (id) => {
      // 这里可以实现选择比较宝可梦的逻辑
      // 可以打开一个弹窗让用户选择
      ElMessage.info('选择比较宝可梦功能待实现')
    }
    
    // 快捷键支持
    const handleKeydown = (event) => {
      if (!pokemon.value) return
      
      switch (event.key) {
        case 'ArrowLeft':
          event.preventDefault()
          navigatePokemon(-1)
          break
        case 'ArrowRight':
          event.preventDefault()
          navigatePokemon(1)
          break
        case 'f':
        case 'F':
          if (!event.ctrlKey && !event.metaKey && !event.altKey) {
            event.preventDefault()
            toggleFavorite()
          }
          break
        case 's':
        case 'S':
          if (!event.ctrlKey && !event.metaKey && !event.altKey) {
            event.preventDefault()
            sharePokemon()
          }
          break
      }
    }
    
    // 导航到相邻宝可梦
    const navigatePokemon = (direction) => {
      const newId = pokemon.value.id + direction
      if (newId > 0) {
        router.push(`/pokemon/${newId}`)
      }
    }

    // 图片类型配置
    const imageTypes = {
      front: '正面',
      back: '背面',
      shiny: '闪光',
      official: '官方立绘'
    }

    // 技能筛选器
    const moveFilters = [
      { key: 'all', label: '全部' },
      { key: 'level-up', label: '升级' },
      { key: 'machine', label: '学习机' },
      { key: 'egg', label: '遗传' },
      { key: 'tutor', label: '教学' }
    ]

    // 当前形态
    const currentForm = computed(() => {
      if (!pokemon.value?.forms) return null
      return pokemon.value.forms.find(f => f.id === selectedFormId.value) || pokemon.value.forms[0]
    })

    // 当前图片URL
    const currentImageUrl = computed(() => {
      const form = currentForm.value
      if (!form) return sprites.pokemon(pokemon.value?.id)

      switch (imageMode.value) {
        case 'back':
          return form.spriteBackUrl || sprites.pokemon(pokemon.value?.id)
        case 'shiny':
          return form.spriteShinyUrl || sprites.pokemon(pokemon.value?.id)
        case 'official':
          return form.officialArtworkUrl || sprites.pokemon(pokemon.value?.id)
        default:
          return form.spriteUrl || sprites.pokemon(pokemon.value?.id)
      }
    })

    // 过滤后的技能
    const filteredMoves = computed(() => {
      if (selectedMoveFilter.value === 'all') return moves.value
      return moves.value.filter(move => {
        const method = move.learnMethod?.toLowerCase() || ''
        return method.includes(selectedMoveFilter.value)
      })
    })

    // 获取图片图标
    const getImageIcon = (type) => {
      const icons = {
        front: Image,
        back: RotateCcw,
        shiny: Shiny,
        official: Star
      }
      return icons[type] || Image
    }

    // 获取性别比例文本
    const getGenderRatioText = (rate) => {
      if (rate === null || rate === undefined) return '未知'
      if (rate === -1) return '无性别'
      if (rate === 0) return '全雄'
      if (rate === 8) return '全雌'
      const female = (rate / 8) * 100
      const male = 100 - female
      return `♂${male}% / ♀${female}%`
    }

    // 获取伤害分类颜色
    const getDamageClassColor = (damageClass) => {
      const colors = {
        '物理': 'bg-red-100 text-red-700',
        '特殊': 'bg-blue-100 text-blue-700',
        '变化': 'bg-green-100 text-green-700'
      }
      return colors[damageClass] || 'bg-gray-100 text-gray-700'
    }

    // 获取宝可梦详情 - 使用缓存
    const fetchPokemonDetail = async () => {
      try {
        const result = await dataCache.getOrFetch('pokemon-detail', { id: route.params.id }, async () => {
          return await pokemonApi.getDetail(route.params.id)
        })
        if (result.code === 200) {
          pokemon.value = result.data
          selectedFormId.value = result.data.forms?.[0]?.id
          imageLoaded.value = false
          
          // 检查收藏状态
          checkFavorite()
          
          // 获取技能
          if (selectedFormId.value) {
            fetchMoves(selectedFormId.value)
          }
        } else {
          ElMessage.error(result.message || '获取详情失败')
          router.push('/pokemon')
        }
      } catch (error) {
        console.error('获取宝可梦详情失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      }
    }

    // 获取技能列表
    const fetchMoves = async (formId) => {
      loadingMoves.value = true
      try {
        const result = await pokemonApi.getFormMoves(formId)
        if (result.code === 200) {
          moves.value = result.data.slice(0, 100)
        }
      } catch (error) {
        console.error('获取技能失败:', error)
      } finally {
        loadingMoves.value = false
      }
    }

    // 获取图片
    const getPokemonImage = (id) => sprites.pokemon(id)
    
    // 图片加载失败
    const handleImageError = (event) => {
      if (event.target) {
        event.target.src = sprites.default
        imageLoaded.value = true
      }
    }

    // 返回
    const goBack = () => router.push('/pokemon')

    // 监听形态变化
    watch(selectedFormId, (newId) => {
      if (newId && pokemon.value) {
        fetchMoves(newId)
      }
    })

    onMounted(() => {
      fetchPokemonDetail()
      loadFavorites()
      window.addEventListener('keydown', handleKeydown)
    })
    
    onUnmounted(() => {
      window.removeEventListener('keydown', handleKeydown)
    })

    return {
      pokemon,
      moves,
      loadingMoves,
      selectedFormId,
      imageLoaded,
      imageMode,
      selectedMoveFilter,
      imageTypes,
      moveFilters,
      currentForm,
      currentImageUrl,
      filteredMoves,
      getImageIcon,
      getGenderRatioText,
      getDamageClassColor,
      getPokemonImage,
      handleImageError,
      goBack,
      isFavorite,
      showCompareModal,
      comparePokemon,
      statsViewMode,
      toggleFavorite,
      sharePokemon,
      selectComparePokemon
    }
  }
}
</script>

<style scoped>
.pokemon-detail {
  padding-bottom: 2rem;
}

/* 模态框动画 */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
}

/* 改进的动画效果 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(59, 130, 246, 0);
  }
}

.pokemon-detail > div {
  animation: fadeInUp 0.5s ease-out;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .pokemon-detail {
    padding-bottom: 1rem;
  }
  
  .fixed.top-4.right-4 {
    top: auto;
    bottom: 4rem;
    right: 1rem;
  }
  
  .bg-white.rounded-3xl.shadow-xl {
    border-radius: 1.5rem;
  }
  
  .text-4xl {
    font-size: 2rem;
  }
  
  .grid.md\:grid-cols-4 {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .grid.md\:grid-cols-3 {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .grid.md\:grid-cols-2 {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .w-72.h-72 {
    width: 180px;
    height: 180px;
  }
  
  .w-28.h-28 {
    width: 80px;
    height: 80px;
  }
  
  .w-20.h-20 {
    width: 60px;
    height: 60px;
  }
  
  .text-2xl {
    font-size: 1.25rem;
  }
  
  .px-4.py-2 {
    padding: 0.5rem 0.75rem;
  }
}

/* 滚动条美化 */
.overflow-y-auto::-webkit-scrollbar {
  width: 8px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* 改进的过渡效果 */
.group:hover .group-hover\:scale-110 {
  transform: scale(1.1);
}

.transition-all.duration-300 {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>