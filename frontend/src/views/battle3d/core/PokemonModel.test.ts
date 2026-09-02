/**
 * @description Test file for PokemonModel
 * @description PokemonModel 的测试文件
 */

import { PokemonEntity, typeColorMap } from './PokemonModel'

// Test type color mapping
// 测试属性颜色映射
console.log('Testing type color mapping:')
console.log('火 type color:', typeColorMap['火'])
console.log('水 type color:', typeColorMap['水'])
console.log('电 type color:', typeColorMap['电'])

// Test PokemonEntity creation
// 测试 PokemonEntity 创建
console.log('\nTesting PokemonEntity creation:')

try {
  const pikachu = new PokemonEntity({
    name: '皮卡丘',
    type: '电',
    currentHp: 100,
    maxHp: 100
  })
  
  console.log('Created Pikachu successfully')
  console.log('Group name:', pikachu.group.name)
  
  // Test position setting
  // 测试位置设置
  pikachu.setPosition(1, 2, 3)
  console.log('Position set to:', pikachu.group.position)
  
  // Test HP bar update
  // 测试血条更新
  pikachu.updateHpBar(80, 100)
  console.log('HP bar updated')
  
  // Test name tag setting
  // 测试名字标签设置
  pikachu.setNameTag('雷丘')
  console.log('Name tag set to: 雷丘')
  
  // Test highlight
  // 测试高亮
  pikachu.setHighlighted(true)
  console.log('Highlighted: true')
  
  // Test animation
  // 测试动画
  pikachu.playAnimation('attack', 1000)
  console.log('Playing attack animation')
  
  // Test dispose
  // 测试清理
  pikachu.dispose()
  console.log('Pokemon disposed')
  
  console.log('\nAll tests passed!')
} catch (error) {
  console.error('Test failed:', error)
}