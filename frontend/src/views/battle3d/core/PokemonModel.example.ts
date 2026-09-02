/**
 * @description Example usage of PokemonModel
 * @description PokemonModel 使用示例
 */

import * as THREE from 'three'
import { PokemonEntity } from './PokemonModel'

// Create a scene
// 创建场景
const scene = new THREE.Scene()
const camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000)
const renderer = new THREE.WebGLRenderer()
renderer.setSize(window.innerWidth, window.innerHeight)
document.body.appendChild(renderer.domElement)

// Create Pokemon entities
// 创建宝可梦实体
const pikachu = new PokemonEntity({
  name: '皮卡丘',
  type: '电',
  currentHp: 100,
  maxHp: 100
})

const charizard = new PokemonEntity({
  name: '喷火龙',
  type: '火',
  currentHp: 150,
  maxHp: 150
})

const blastoise = new PokemonEntity({
  name: '水箭龟',
  type: '水',
  currentHp: 120,
  maxHp: 120
})

// Position Pokemon
// 设置宝可梦位置
pikachu.setPosition(-3, 0, 0)
charizard.setPosition(0, 0, 0)
blastoise.setPosition(3, 0, 0)

// Add to scene
// 添加到场景
scene.add(pikachu.group)
scene.add(charizard.group)
scene.add(blastoise.group)

// Set camera position
// 设置相机位置
camera.position.set(0, 5, 10)
camera.lookAt(0, 0, 0)

// Add lighting
// 添加光照
const ambientLight = new THREE.AmbientLight(0x404040, 0.6)
scene.add(ambientLight)

const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
directionalLight.position.set(10, 20, 10)
scene.add(directionalLight)

// Animation loop
// 动画循环
function animate() {
  requestAnimationFrame(animate)
  
  // Rotate camera around scene
  // 相机绕场景旋转
  const time = Date.now() * 0.001
  camera.position.x = Math.sin(time) * 10
  camera.position.z = Math.cos(time) * 10
  camera.lookAt(0, 0, 0)
  
  renderer.render(scene, camera)
}

animate()

// Example: Play animations
// 示例：播放动画
setTimeout(() => {
  pikachu.playAnimation('attack', 1000)
}, 2000)

setTimeout(() => {
  charizard.playAnimation('hit', 500)
}, 4000)

setTimeout(() => {
  blastoise.playAnimation('heal', 1500)
}, 6000)

// Example: Update HP
// 示例：更新生命值
setTimeout(() => {
  pikachu.updateHpBar(50, 100)
}, 3000)

// Example: Highlight Pokemon
// 示例：高亮宝可梦
setTimeout(() => {
  pikachu.setHighlighted(true)
}, 5000)

setTimeout(() => {
  pikachu.setHighlighted(false)
}, 7000)

// Example: Change name tag
// 示例：更改名字标签
setTimeout(() => {
  pikachu.setNameTag('雷丘')
}, 8000)

// Cleanup function
// 清理函数
function cleanup() {
  pikachu.dispose()
  charizard.dispose()
  blastoise.dispose()
  renderer.dispose()
}

// Call cleanup when page unloads
// 页面卸载时调用清理
window.addEventListener('beforeunload', cleanup)