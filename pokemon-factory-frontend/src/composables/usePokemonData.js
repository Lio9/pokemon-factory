import { ref, reactive } from 'vue'
import { pokemonApi } from '../services/api'

/**
 * 宝可梦数据获取组合式函数
 */
export function usePokemonData() {
  const pokemon = ref(null)
  const moves = ref([])
  const abilities = ref([])
  const evolutions = ref([])
  const loading = ref(false)
  const error = ref(null)

  // 重置数据
  const reset = () => {
    pokemon.value = null
    moves.value = []
    abilities.value = []
    evolutions.value = []
    loading.value = false
    error.value = null
  }

  // 获取宝可梦详情
  const fetchPokemonDetail = async (id) => {
    reset()
    loading.value = true
    error.value = null
    
    try {
      const result = await pokemonApi.getDetail(id)
      
      if (result.code === 200) {
        pokemon.value = result.data
        // 获取关联数据
        await Promise.all([
          fetchMoves(id),
          fetchAbilities(id),
          fetchEvolutions(id)
        ])
      } else {
        throw new Error(result.message || '获取详情失败')
      }
    } catch (err) {
      error.value = err.message
      console.error('获取宝可梦详情失败:', err)
    } finally {
      loading.value = false
    }
  }

  // 获取技能数据
  const fetchMoves = async (id) => {
    try {
      const result = await pokemonApi.getMoves(id)
      if (result.code === 200) {
        moves.value = result.data
      }
    } catch (err) {
      console.error('获取技能失败:', err)
      // 使用示例数据
      useSampleMoves(id)
    }
  }

  // 获取特性数据
  const fetchAbilities = async (id) => {
    try {
      const result = await pokemonApi.getAbilities(id)
      if (result.code === 200) {
        abilities.value = result.data
      }
    } catch (err) {
      console.error('获取特性失败:', err)
      // 使用示例数据
      useSampleAbilities(id)
    }
  }

  // 获取进化链数据
  const fetchEvolutions = async (id) => {
    try {
      const result = await pokemonApi.getEvolutionChain(id)
      if (result.code === 200) {
        evolutions.value = result.data
      }
    } catch (err) {
      console.error('获取进化链失败:', err)
      // 使用示例数据
      useSampleEvolutions(id)
    }
  }

  // 使用示例技能数据
  const useSampleMoves = (id) => {
    const pokemonId = parseInt(id) || 1
    let sampleMoves = []
    
    switch(pokemonId) {
      case 1: // 妙蛙种子
        sampleMoves = [
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
          },
          {
            id: 13,
            name: '日光束',
            nameEn: 'solar beam',
            type: '草',
            category: '特殊',
            learnMethod: '技能机',
            levelOrMachine: 'TM06',
            power: '120',
            accuracy: '100',
            pp: '10',
            description: '先照射阳光，然后发射强烈的光束攻击对手。'
          },
          {
            id: 14,
            name: '能量球',
            nameEn: 'energy ball',
            type: '草',
            category: '特殊',
            learnMethod: '技能机',
            levelOrMachine: 'TM03',
            power: '90',
            accuracy: '100',
            pp: '10',
            description: '发射能量球攻击对手，有时会降低对手的特防。'
          }
        ]
        break
      case 4: // 小火龙
        sampleMoves = [
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
          },
          {
            id: 9,
            name: '火焰拳',
            nameEn: 'fire punch',
            type: '火',
            category: '物理',
            learnMethod: '升级',
            levelOrMachine: '13',
            power: '75',
            accuracy: '100',
            pp: '15',
            description: '让拳头燃烧火焰向对手突击，有时会让对手陷入灼伤状态。'
          },
          {
            id: 10,
            name: '火焰旋涡',
            nameEn: 'fire spin',
            type: '火',
            category: '特殊',
            learnMethod: '升级',
            levelOrMachine: '16',
            power: '35',
            accuracy: '85',
            pp: '15',
            description: '在对手身上燃烧火焰，造成伤害。'
          },
          {
            id: 11,
            name: '喷射火焰',
            nameEn: 'flamethrower',
            type: '火',
            category: '特殊',
            learnMethod: '技能机',
            levelOrMachine: 'TM35',
            power: '90',
            accuracy: '100',
            pp: '15',
            description: '喷射出强大的火焰攻击对手。'
          },
          {
            id: 12,
            name: '龙之怒',
            nameEn: 'dragon rage',
            type: '龙',
            category: '特殊',
            learnMethod: '升级',
            levelOrMachine: '19',
            power: '-',
            accuracy: '100',
            pp: '10',
            description: '用龙的力量发出冲击波，造成固定伤害。'
          },
          {
            id: 13,
            name: '龙之舞',
            nameEn: 'dragon dance',
            type: '龙',
            category: '变化',
            learnMethod: '技能机',
            levelOrMachine: 'TM43',
            power: '-',
            accuracy: '-',
            pp: '20',
            description: '提高攻击和速度，但降低防御。'
          },
          {
            id: 14,
            name: '火焰冲击',
            nameEn: 'fire blast',
            type: '火',
            category: '特殊',
            learnMethod: '技能机',
            levelOrMachine: 'TM27',
            power: '110',
            accuracy: '85',
            pp: '5',
            description: '喷射出强大的火焰攻击对手，有时会让对手陷入灼伤状态。'
          }
        ]
        break
      case 7: // 杰尼龟
        sampleMoves = [
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
            description: '在对手身上燃烧火焰，造成伤害。'
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
          },
          {
            id: 10,
            name: '水炮',
            nameEn: 'hydro pump',
            type: '水',
            category: '特殊',
            learnMethod: '技能机',
            levelOrMachine: 'TM03',
            power: '110',
            accuracy: '80',
            pp: '5',
            description: '发射出强大的水流攻击对手。'
          },
          {
            id: 11,
            name: '冰冻光束',
            nameEn: 'ice beam',
            type: '冰',
            category: '特殊',
            learnMethod: '技能机',
            levelOrMachine: 'TM13',
            power: '90',
            accuracy: '100',
            pp: '10',
            description: '发射出强大的冰冻光线攻击对手，有时会让对手陷入冰冻状态。'
          },
          {
            id: 12,
            name: '暴风雪',
            nameEn: 'blizzard',
            type: '冰',
            category: '特殊',
            learnMethod: '技能机',
            levelOrMachine: 'TM04',
            power: '110',
            accuracy: '70',
            pp: '5',
            description: '召唤暴风雪攻击对手，有时会让对手陷入冰冻状态。'
          },
          {
            id: 13,
            name: '水流喷射',
            nameEn: 'surf',
            type: '水',
            category: '特殊',
            learnMethod: '技能机',
            levelOrMachine: 'TM11',
            power: '90',
            accuracy: '100',
            pp: '15',
            description: '召唤出巨大的水流攻击对手。'
          },
          {
            id: 14,
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
          }
        ]
        break
      default:
        sampleMoves = [
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
          },
          {
            id: 3,
            name: '瞪眼',
            nameEn: 'leer',
            type: '一般',
            category: '变化',
            power: '-',
            accuracy: '100',
            pp: '30',
            description: '瞪大眼睛，降低对手的防御。'
          },
          {
            id: 4,
            name: '闪光',
            nameEn: 'flash',
            type: '一般',
            category: '变化',
            power: '-',
            accuracy: '100',
            pp: '20',
            description: '发出强烈的闪光，降低对手的命中率。'
          },
          {
            id: 5,
            name: '起死回生',
            nameEn: 'revival',
            type: '一般',
            category: '变化',
            power: '-',
            accuracy: '-',
            pp: '10',
            description: '让已经倒下的宝可梦重新站起来。'
          },
          {
            id: 6,
            name: '觉醒力量',
            nameEn: 'awakening',
            type: '一般',
            category: '变化',
            power: '-',
            accuracy: '-',
            pp: '15',
            description: '让陷入睡眠的宝可梦醒来。'
          }
        ]
    }
    
    moves.value = sampleMoves
  }

  // 使用示例特性数据
  const useSampleAbilities = (id) => {
    const pokemonId = parseInt(id) || 1
    let sampleAbilities = []
    
    switch(pokemonId) {
      case 1: // 妙蛙种子
        sampleAbilities = [
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
        ]
        break
      case 4: // 小火龙
        sampleAbilities = [
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
        ]
        break
      case 7: // 杰尼龟
        sampleAbilities = [
          {
            id: 1,
            name: '激流',
            nameEn: 'torrent',
            description: 'ＨＰ减少的时候，水属性的招式威力会提高。',
            isHidden: false,
            slot: 1
          },
          {
            id: 2,
            name: '水分',
            nameEn: 'rain dish',
            description: '下雨天气时，每回合回复ＨＰ。',
            isHidden: true,
            slot: 3
          }
        ]
        break
      default:
        sampleAbilities = [
          {
            id: 1,
            name: '特性1',
            nameEn: 'ability1',
            description: '这是默认特性描述。',
            isHidden: false,
            slot: 1
          },
          {
            id: 2,
            name: '特性2',
            nameEn: 'ability2',
            description: '这是第二个特性描述。',
            isHidden: false,
            slot: 2
          },
          {
            id: 3,
            name: '隐藏特性',
            nameEn: 'hidden ability',
            description: '这是隐藏特性描述。',
            isHidden: true,
            slot: 3
          }
        ]
    }
    
    abilities.value = sampleAbilities
  }

  // 使用示例进化链数据
  const useSampleEvolutions = (id) => {
    const pokemonId = parseInt(id) || 1
    let sampleEvolutions = []
    
    switch(pokemonId) {
      case 1: // 妙蛙种子进化链
        sampleEvolutions = [
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
        ]
        break
      case 4: // 小火龙进化链
        sampleEvolutions = [
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
        ]
        break
      case 7: // 杰尼龟进化链
        sampleEvolutions = [
          {
            id: 1,
            pokemonId: 7,
            pokemonName: '杰尼龟',
            pokemonIndexNumber: '0007',
            evolvesFromId: null,
            evolvesFromName: null,
            evolutionMethod: '升级',
            evolutionParameter: '等级',
            evolutionValue: '16'
          },
          {
            id: 2,
            pokemonId: 8,
            pokemonName: '卡咪龟',
            pokemonIndexNumber: '0008',
            evolvesFromId: 7,
            evolvesFromName: '杰尼龟',
            evolutionMethod: '升级',
            evolutionParameter: '等级',
            evolutionValue: '36'
          },
          {
            id: 3,
            pokemonId: 9,
            pokemonName: '水箭龟',
            pokemonIndexNumber: '0009',
            evolvesFromId: 8,
            evolvesFromName: '卡咪龟',
            evolutionMethod: '升级',
            evolutionParameter: '等级',
            evolutionValue: '-1'
          }
        ]
        break
      default:
        sampleEvolutions = [
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
          },
          {
            id: 2,
            pokemonId: pokemonId + 1,
            pokemonName: '进化形态' + (pokemonId + 1),
            pokemonIndexNumber: String(pokemonId + 1).padStart(4, '0'),
            evolvesFromId: pokemonId,
            evolvesFromName: '宝可梦' + pokemonId,
            evolutionMethod: '等级',
            evolutionParameter: '等级',
            evolutionValue: '20'
          }
        ]
    }
    
    evolutions.value = sampleEvolutions
  }

  return {
    pokemon,
    moves,
    abilities,
    evolutions,
    loading,
    error,
    fetchPokemonDetail,
    reset
  }
}