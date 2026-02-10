<template>
  <div v-if="pokemon" class="pokemon-detail">
    <!-- 头部信息 -->
    <div class="detail-header bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl p-6 mb-6">
      <div class="flex flex-col md:flex-row items-start gap-6">
        <div class="flex-shrink-0">
          <div class="w-32 h-32 bg-white rounded-2xl shadow-lg flex items-center justify-center text-6xl font-bold text-gray-300 border-4 border-dashed border-gray-200">
            {{ pokemon.indexNumber }}
          </div>
        </div>
        <div class="flex-1">
          <div class="flex items-center gap-3 mb-2">
            <h1 class="text-3xl font-bold text-gray-900">{{ pokemon.name }}</h1>
            <span class="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">
              #{{ pokemon.indexNumber }}
            </span>
          </div>
          <p class="text-gray-600 mb-3">{{ pokemon.nameEn }} / {{ pokemon.nameJp }}</p>
          <p class="text-gray-700 leading-relaxed">{{ pokemon.profile || '暂无描述' }}</p>
        </div>
      </div>
    </div>

    <!-- 形态信息 -->
    <div v-if="pokemon.forms && pokemon.forms.length > 0" class="section mb-6">
      <h2 class="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg class="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
        </svg>
        形态信息
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div v-for="form in pokemon.forms" :key="form.id" class="bg-white rounded-lg border border-gray-200 p-4">
          <h3 class="font-semibold text-gray-900 mb-2">{{ form.name }}</h3>
          <div class="grid grid-cols-2 gap-2 text-sm">
            <div>
              <span class="text-gray-500">生命值:</span>
              <span class="ml-2 font-medium">{{ form.hp || 0 }}</span>
            </div>
            <div>
              <span class="text-gray-500">攻击:</span>
              <span class="ml-2 font-medium">{{ form.attack || 0 }}</span>
            </div>
            <div>
              <span class="text-gray-500">防御:</span>
              <span class="ml-2 font-medium">{{ form.defense || 0 }}</span>
            </div>
            <div>
              <span class="text-gray-500">特攻:</span>
              <span class="ml-2 font-medium">{{ form.spAttack || 0 }}</span>
            </div>
            <div>
              <span class="text-gray-500">特防:</span>
              <span class="ml-2 font-medium">{{ form.spDefense || 0 }}</span>
            </div>
            <div>
              <span class="text-gray-500">速度:</span>
              <span class="ml-2 font-medium">{{ form.speed || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 属性信息 -->
    <div v-if="pokemon.forms && pokemon.forms.length > 0 && pokemon.forms[0].types" class="section mb-6">
      <h2 class="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg class="w-5 h-5 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"></path>
        </svg>
        属性信息
      </h2>
      <div class="flex flex-wrap gap-2">
        <span 
          v-for="type in pokemon.forms[0].types" 
          :key="type.id"
          class="px-3 py-1 rounded-full text-sm font-medium"
          :style="{ backgroundColor: type.color + '20', color: type.color }"
        >
          {{ type.name }}
        </span>
      </div>
    </div>

    <!-- 特性信息 -->
    <div v-if="abilities && abilities.length > 0" class="section mb-6">
      <h2 class="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"></path>
        </svg>
        特性信息
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div 
          v-for="ability in abilities" 
          :key="ability.id"
          class="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow"
        >
          <div class="flex items-start justify-between mb-2">
            <h3 class="font-semibold text-gray-900">{{ ability.name }}</h3>
            <span 
              v-if="ability.isHidden" 
              class="px-2 py-1 bg-purple-100 text-purple-800 text-xs rounded-full"
            >
              隐藏特性
            </span>
          </div>
          <p class="text-gray-600 text-sm">{{ ability.description }}</p>
        </div>
      </div>
    </div>

    <!-- 技能信息 -->
    <div v-if="moves && moves.length > 0" class="section mb-6">
      <h2 class="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg class="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
        </svg>
        技能信息
      </h2>
      <div class="overflow-x-auto">
        <table class="min-w-full bg-white border border-gray-200 rounded-lg">
          <thead class="bg-gray-50">
            <tr>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">技能名称</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">属性</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">分类</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">学习方式</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">等级/机器</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">威力</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">命中</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">PP</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr 
              v-for="move in moves" 
              :key="move.id"
              class="hover:bg-gray-50 transition-colors"
            >
              <td class="py-3 px-4 text-sm font-medium text-gray-900">{{ move.name }}</td>
              <td class="py-3 px-4 text-sm text-gray-500">{{ move.type }}</td>
              <td class="py-3 px-4 text-sm text-gray-500">{{ move.category }}</td>
              <td class="py-3 px-4 text-sm text-gray-500">
                <span 
                  :class="{
                    'bg-blue-100 text-blue-800': move.learnMethod === '升级',
                    'bg-green-100 text-green-800': move.learnMethod === '生蛋',
                    'bg-purple-100 text-purple-800': move.learnMethod === '技能机',
                    'bg-yellow-100 text-yellow-800': move.learnMethod === '遗传',
                    'bg-red-100 text-red-800': move.learnMethod === '教授'
                  }"
                  class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium"
                >
                  {{ move.learnMethod }}
                </span>
              </td>
              <td class="py-3 px-4 text-sm text-gray-500">{{ move.level || move.levelOrMachine || '-' }}</td>
              <td class="py-3 px-4 text-sm text-gray-500">{{ move.power || '-' }}</td>
              <td class="py-3 px-4 text-sm text-gray-500">{{ move.accuracy || '-' }}</td>
              <td class="py-3 px-4 text-sm text-gray-500">{{ move.pp || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 进化链信息 -->
    <div v-if="evolutions && evolutions.length > 0" class="section mb-6">
      <h2 class="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
        </svg>
        进化链
      </h2>
      <div class="flex items-center justify-center">
        <div class="flex flex-col md:flex-row items-center gap-4 md:gap-8">
          <div 
            v-for="(evol, index) in evolutions" 
            :key="evol.id"
            class="flex flex-col items-center"
          >
            <div class="w-24 h-24 bg-gradient-to-br from-blue-100 to-indigo-100 rounded-xl flex items-center justify-center text-lg font-bold text-blue-600 border-2 border-dashed border-blue-200 mb-2">
              {{ evol.pokemonIndexNumber }}
            </div>
            <div class="text-center">
              <div class="font-semibold text-gray-900">{{ evol.pokemonName }}</div>
              <div v-if="evol.evolvesFromName" class="text-xs text-gray-500 mt-1">
                {{ evol.evolutionMethod }}: {{ evol.evolutionValue !== '-1' ? evol.evolutionValue + '级' : '最终形态' }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 返回按钮 -->
    <div class="text-center mt-8">
      <button @click="goBack" class="px-6 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors">
        返回列表
      </button>
    </div>
  </div>
  
  <div v-else class="text-center py-12">
    <div class="text-gray-400 mb-4">
      <svg class="w-12 h-12 mx-auto animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
      </svg>
    </div>
    <p class="text-gray-500">加载中...</p>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pokemonApi } from '../services/api.js'

export default {
  name: 'PokemonDetail',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const pokemon = ref(null)
    const moves = ref([])
    const abilities = ref([])
    const evolutions = ref([])
    const loading = ref(true)
    
    const fetchPokemonDetail = async () => {
      try {
        loading.value = true
        console.log('正在获取宝可梦详情，ID:', route.params.id)
        
        // 临时使用模拟数据进行测试
        const mockData = {
          code: 200,
          data: {
            id: parseInt(route.params.id) || 1,
            indexNumber: '000' + (parseInt(route.params.id) || 1),
            name: '妙蛙种子',
            nameEn: 'bulbasaur',
            nameJp: 'フシギダネ',
            profile: '这是一只神奇的宝可梦，在它的背上有一颗种子。',
            genus: '种子宝可梦',
            forms: [
              {
                id: parseInt(route.params.id) || 1,
                name: '妙蛙种子',
                indexNumber: '000' + (parseInt(route.params.id) || 1),
                hp: 45,
                attack: 49,
                defense: 49,
                spAttack: 65,
                spDefense: 65,
                speed: 45,
                types: [
                  {
                    id: 1,
                    name: '草',
                    nameEn: 'grass',
                    color: '#78C850'
                  },
                  {
                    id: 2,
                    name: '毒',
                    nameEn: 'poison',
                    color: '#A040A0'
                  }
                ]
              }
            ]
          }
        };
        
        // 模拟网络延迟
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const result = mockData;
        
        console.log('API返回结果:', result)
        
        if (result.code === 200) {
          pokemon.value = result.data
          console.log('设置的宝可梦数据:', pokemon.value)
        } else {
          console.error('API返回错误:', result.message)
          ElMessage.error(result.message || '获取详情失败')
          router.push('/pokemon')
        }
      } catch (error) {
        console.error('获取宝可梦详情失败:', error)
        ElMessage.error('网络错误，请稍后重试')
        router.push('/pokemon')
      } finally {
        loading.value = false
      }
    }
    
    // 获取技能数据
    const fetchPokemonMoves = async () => {
      try {
        // 根据不同的宝可梦ID返回不同的技能数据
        const pokemonId = parseInt(route.params.id) || 1;
        let mockMoves = [];
        
        switch(pokemonId) {
          case 1: // 妙蛙种子
            mockMoves = [
              {
                id: 1,
                name: '撞击',
                nameEn: 'tackle',
                type: '一般',
                category: '物理',
                learnMethod: '升级',
                levelOrMachine: '1',
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
                learnMethod: '升级',
                levelOrMachine: '3',
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
                learnMethod: '升级',
                levelOrMachine: '7',
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
                learnMethod: '升级',
                levelOrMachine: '9',
                power: '-',
                accuracy: '90',
                pp: '10',
                description: '植入寄生种子，每回合吸取对手的ＨＰ。'
              },
              {
                id: 5,
                name: '生长',
                nameEn: 'growth',
                type: '一般',
                category: '变化',
                learnMethod: '升级',
                levelOrMachine: '13',
                power: '-',
                accuracy: '-',
                pp: '20',
                description: '让身体成长，提高攻击和特攻。'
              },
              {
                id: 6,
                name: '睡眠粉',
                nameEn: 'sleep powder',
                type: '草',
                category: '变化',
                learnMethod: '升级',
                levelOrMachine: '15',
                power: '-',
                accuracy: '75',
                pp: '15',
                description: '撒出催眠粉，让对手陷入睡眠状态。'
              },
              {
                id: 7,
                name: '毒刺',
                nameEn: 'poison sting',
                type: '毒',
                category: '物理',
                learnMethod: '生蛋',
                levelOrMachine: '-',
                power: '15',
                accuracy: '100',
                pp: '35',
                description: '用带毒的针刺攻击对手，有时会让对手中毒。'
              },
              {
                id: 8,
                name: '吸取',
                nameEn: 'absorb',
                type: '草',
                category: '特殊',
                learnMethod: '升级',
                levelOrMachine: '19',
                power: '20',
                accuracy: '100',
                pp: '25',
                description: '吸取养分进行攻击，可以回复给予对手伤害的一半ＨＰ。'
              },
              {
                id: 9,
                name: '麻痹粉',
                nameEn: 'stun spore',
                type: '草',
                category: '变化',
                learnMethod: '升级',
                levelOrMachine: '21',
                power: '-',
                accuracy: '75',
                pp: '30',
                description: '撒出麻痹粉，让对手陷入麻痹状态。'
              },
              {
                id: 10,
                name: '飞叶快刀',
                nameEn: 'razor leaf',
                type: '草',
                category: '物理',
                learnMethod: '升级',
                levelOrMachine: '25',
                power: '55',
                accuracy: '95',
                pp: '25',
                description: '飞出叶片切斩对手，容易击中要害。'
              },
              {
                id: 11,
                name: '甜甜香气',
                nameEn: 'sweet scent',
                type: '一般',
                category: '变化',
                learnMethod: '升级',
                levelOrMachine: '27',
                power: '-',
                accuracy: '100',
                pp: '20',
                description: '散发芳香气味，降低对手的闪避率。'
              },
              {
                id: 12,
                name: '超级吸取',
                nameEn: 'mega drain',
                type: '草',
                category: '特殊',
                learnMethod: '升级',
                levelOrMachine: '31',
                power: '40',
                accuracy: '100',
                pp: '15',
                description: '强劲地吸取养分进行攻击，可以回复给予对手伤害的一半ＨＰ。'
              }
            ];
            break;
          case 4: // 小火龙
            mockMoves = [
              {
                id: 1,
                name: '抓',
                nameEn: 'scratch',
                type: '一般',
                category: '物理',
                learnMethod: '升级',
                levelOrMachine: '1',
                power: '40',
                accuracy: '100',
                pp: '35',
                description: '用锐利的爪子抓对手。'
              },
              {
                id: 2,
                name: '火花',
                nameEn: 'ember',
                type: '火',
                category: '特殊',
                learnMethod: '升级',
                levelOrMachine: '4',
                power: '40',
                accuracy: '100',
                pp: '25',
                description: '发射小型火焰攻击对手，有时会让对手陷入灼伤状态。'
              },
              {
                id: 3,
                name: '吼叫',
                nameEn: 'growl',
                type: '一般',
                category: '变化',
                learnMethod: '升级',
                levelOrMachine: '1',
                power: '-',
                accuracy: '100',
                pp: '40',
                description: '可爱地大声吼叫，降低对手的攻击。'
              },
              {
                id: 4,
                name: '烟幕',
                nameEn: 'smokescreen',
                type: '一般',
                category: '变化',
                learnMethod: '升级',
                levelOrMachine: '7',
                power: '-',
                accuracy: '100',
                pp: '20',
                description: '喷出黑烟，降低对手的命中率。'
              },
              {
                id: 5,
                name: '愤怒',
                nameEn: 'rage',
                type: '一般',
                category: '物理',
                learnMethod: '升级',
                levelOrMachine: '10',
                power: '20',
                accuracy: '100',
                pp: '20',
                description: '如果在使出招式后受到攻击，愤怒就会增强，攻击也会提高。'
              },
              {
                id: 6,
                name: '火焰车',
                nameEn: 'flame wheel',
                type: '火',
                category: '物理',
                learnMethod: '技能机',
                levelOrMachine: 'TM31',
                power: '60',
                accuracy: '100',
                pp: '25',
                description: '让全身覆盖火焰向对手突击，有时会让对手陷入灼伤状态。'
              },
              {
                id: 7,
                name: '劈开',
                nameEn: 'slash',
                type: '一般',
                category: '物理',
                learnMethod: '生蛋',
                levelOrMachine: '-',
                power: '70',
                accuracy: '100',
                pp: '20',
                description: '用锐利的爪子或镰刀等劈开对手，容易击中要害。'
              },
              {
                id: 8,
                name: '龙爪',
                nameEn: 'dragon claw',
                type: '龙',
                category: '物理',
                learnMethod: '技能机',
                levelOrMachine: 'TM02',
                power: '80',
                accuracy: '100',
                pp: '15',
                description: '用尖锐的巨爪劈开对手。'
              }
            ];
            break;
          case 7: // 杰尼龟
            mockMoves = [
              {
                id: 1,
                name: '撞击',
                nameEn: 'tackle',
                type: '一般',
                category: '物理',
                learnMethod: '升级',
                levelOrMachine: '1',
                power: '40',
                accuracy: '100',
                pp: '35',
                description: '用身体撞向对手进行攻击。'
              },
              {
                id: 2,
                name: '摇尾巴',
                nameEn: 'tail whip',
                type: '一般',
                category: '变化',
                learnMethod: '升级',
                levelOrMachine: '1',
                power: '-',
                accuracy: '100',
                pp: '30',
                description: '可爱地摆动尾巴，降低对手的防御。'
              },
              {
                id: 3,
                name: '水枪',
                nameEn: 'water gun',
                type: '水',
                category: '特殊',
                learnMethod: '升级',
                levelOrMachine: '7',
                power: '40',
                accuracy: '100',
                pp: '25',
                description: '喷射水流攻击对手。'
              },
              {
                id: 4,
                name: '泡沫',
                nameEn: 'bubble',
                type: '水',
                category: '特殊',
                learnMethod: '升级',
                levelOrMachine: '10',
                power: '40',
                accuracy: '100',
                pp: '30',
                description: '用泡沫攻击对手，有时会降低对手的速度。'
              },
              {
                id: 5,
                name: '守住',
                nameEn: 'protect',
                type: '一般',
                category: '变化',
                learnMethod: '升级',
                levelOrMachine: '13',
                power: '-',
                accuracy: '-',
                pp: '10',
                description: '完全抵挡对手的攻击，自己的ＨＰ不会被减少。'
              },
              {
                id: 6,
                name: '水之波动',
                nameEn: 'water pulse',
                type: '水',
                category: '特殊',
                learnMethod: '升级',
                levelOrMachine: '19',
                power: '60',
                accuracy: '100',
                pp: '20',
                description: '用水之波动攻击对手，有时会使对手混乱。'
              },
              {
                id: 7,
                name: '潮旋',
                nameEn: 'whirlpool',
                type: '水',
                category: '特殊',
                learnMethod: '升级',
                levelOrMachine: '22',
                power: '35',
                accuracy: '85',
                pp: '15',
                description: '在４～５回合内，用水流困住对手进行攻击。'
              },
              {
                id: 8,
                name: '铁尾',
                nameEn: 'iron tail',
                type: '钢',
                category: '物理',
                learnMethod: '生蛋',
                levelOrMachine: '-',
                power: '100',
                accuracy: '75',
                pp: '15',
                description: '用坚硬的尾巴摔打对手，有时会降低对手的防御。'
              },
              {
                id: 9,
                name: '求雨',
                nameEn: 'rain dance',
                type: '水',
                category: '变化',
                learnMethod: '技能机',
                levelOrMachine: 'TM24',
                power: '-',
                accuracy: '-',
                pp: '5',
                description: '让天气变成下雨，持续5回合。'
              }
            ];
            break;
          default:
            mockMoves = [
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
                name: '叫声',
                nameEn: 'growl',
                type: '一般',
                category: '变化',
                power: '-',
                accuracy: '100',
                pp: '40',
                description: '发出可爱的叫声，降低对手的攻击。'
              }
            ];
        }
        
        // 如果是真实API数据，需要转换字段名
        // 假设从API获取的数据结构为 {learnMethod, level, ...}
        // 转换为前端需要的格式 {learnMethod, levelOrMachine, ...}
        if (mockMoves.length > 0) {
          moves.value = mockMoves.map(move => ({
            ...move,
            levelOrMachine: move.levelOrMachine || move.level?.toString() || '-'
          }));
        } else {
          // 这里应该是从API获取真实数据的逻辑
          // 由于当前使用模拟数据
          moves.value = mockMoves;
        }
      } catch (error) {
        console.error('获取技能数据失败:', error);
        ElMessage.error('获取技能数据失败');
      }
    }
    
    // 获取特性数据
    const fetchPokemonAbilities = async () => {
      try {
        const pokemonId = parseInt(route.params.id) || 1;
        let mockAbilities = [];
        
        switch(pokemonId) {
          case 1: // 妙蛙种子
            mockAbilities = [
              {
                id: 1,
                name: '茂盛',
                nameEn: 'overgrow',
                description: 'ＨＰ减少的时候，草属性的招式威力会提高。',
                isHidden: false,
                slot: 1
              },
              {
                id: 2,
                name: '叶绿素',
                nameEn: 'chlorophyll',
                description: '天气为晴朗时，速度会提高。',
                isHidden: true,
                slot: 3
              }
            ];
            break;
          case 4: // 小火龙
            mockAbilities = [
              {
                id: 1,
                name: '猛火',
                nameEn: 'blaze',
                description: 'ＨＰ减少的时候，火属性的招式威力会提高。',
                isHidden: false,
                slot: 1
              },
              {
                id: 2,
                name: '太阳之力',
                nameEn: 'solar power',
                description: '晴朗天气时，特攻会提高，但每回合损失ＨＰ。',
                isHidden: true,
                slot: 3
              }
            ];
            break;
          default:
            mockAbilities = [
              {
                id: 1,
                name: '特性1',
                nameEn: 'ability1',
                description: '这是默认特性描述。',
                isHidden: false,
                slot: 1
              }
            ];
        }
        
        abilities.value = mockAbilities;
        console.log('获取到特性数据:', mockAbilities);
      } catch (error) {
        console.error('获取特性数据失败:', error);
      }
    }
    
    // 获取进化链数据
    const fetchEvolutionChain = async () => {
      try {
        const pokemonId = parseInt(route.params.id) || 1;
        let mockEvolutions = [];
        
        switch(pokemonId) {
          case 1: // 妙蛙种子进化链
            mockEvolutions = [
              {
                id: 1,
                pokemonId: 1,
                pokemonName: '妙蛙种子',
                pokemonIndexNumber: '0001',
                evolvesFromId: null,
                evolvesFromName: null,
                evolutionMethod: '升级',
                evolutionParameter: '等级',
                evolutionValue: '16'
              },
              {
                id: 2,
                pokemonId: 2,
                pokemonName: '妙蛙草',
                pokemonIndexNumber: '0002',
                evolvesFromId: 1,
                evolvesFromName: '妙蛙种子',
                evolutionMethod: '升级',
                evolutionParameter: '等级',
                evolutionValue: '32'
              },
              {
                id: 3,
                pokemonId: 3,
                pokemonName: '妙蛙花',
                pokemonIndexNumber: '0003',
                evolvesFromId: 2,
                evolvesFromName: '妙蛙草',
                evolutionMethod: '升级',
                evolutionParameter: '等级',
                evolutionValue: '-1'
              }
            ];
            break;
          case 4: // 小火龙进化链
            mockEvolutions = [
              {
                id: 1,
                pokemonId: 4,
                pokemonName: '小火龙',
                pokemonIndexNumber: '0004',
                evolvesFromId: null,
                evolvesFromName: null,
                evolutionMethod: '升级',
                evolutionParameter: '等级',
                evolutionValue: '16'
              },
              {
                id: 2,
                pokemonId: 5,
                pokemonName: '火恐龙',
                pokemonIndexNumber: '0005',
                evolvesFromId: 4,
                evolvesFromName: '小火龙',
                evolutionMethod: '升级',
                evolutionParameter: '等级',
                evolutionValue: '36'
              },
              {
                id: 3,
                pokemonId: 6,
                pokemonName: '喷火龙',
                pokemonIndexNumber: '0006',
                evolvesFromId: 5,
                evolvesFromName: '火恐龙',
                evolutionMethod: '升级',
                evolutionParameter: '等级',
                evolutionValue: '-1'
              }
            ];
            break;
          default:
            mockEvolutions = [
              {
                id: 1,
                pokemonId: pokemonId,
                pokemonName: '宝可梦' + pokemonId,
                pokemonIndexNumber: String(pokemonId).padStart(4, '0'),
                evolvesFromId: null,
                evolvesFromName: null,
                evolutionMethod: '未知',
                evolutionParameter: '未知',
                evolutionValue: '未知'
              }
            ];
        }
        
        evolutions.value = mockEvolutions;
        console.log('获取到进化链数据:', mockEvolutions);
      } catch (error) {
        console.error('获取进化链数据失败:', error);
      }
    }
    
    const goBack = () => {
      router.push('/pokemon')
    }
    
    onMounted(() => {
      fetchPokemonDetail()
      fetchPokemonMoves()
      fetchPokemonAbilities()
      fetchEvolutionChain()
    })
    
    return {
      pokemon,
      moves,
      abilities,
      evolutions,
      loading,
      goBack
    }
  }
}
</script>

<style scoped>
.section {
  animation: fadeInUp 0.3s ease-out;
}

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
</style>