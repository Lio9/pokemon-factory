/**
 * 3D 战场交互管理 Composable
 * @description 处理 3D 战场中的所有用户交互，包括点击、悬停和键盘操作
 * @module useInteraction
 * @version 1.0.0
 * @author MiMo
 * 
 * @changelog
 * v1.0.0 - 初始版本
 * - 支持 Raycaster 3D 物体拾取
 * - 支持键盘快捷键操作
 * - 支持交互模式切换
 */

import { ref, type Ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'

/**
 * 宝可梦实体接口
 * Pokemon entity interface
 */
interface PokemonEntity {
  id: string
  model: THREE.Object3D
  isAlly: boolean
  position: THREE.Vector3
}

/**
 * 交互模式类型
 * Interaction mode type
 * @typedef {'select' | 'target' | 'confirm'} InteractionMode
 */
type InteractionMode = 'select' | 'target' | 'confirm'

/**
 * 行动类型
 * Action type
 * @typedef {'move' | 'switch' | 'special'} ActionType
 */
type ActionType = 'move' | 'switch' | 'special'

/**
 * 点击回调函数类型
 * Click callback function type
 * @typedef {(pokemonId: string) => void} PokemonClickCallback
 */
type PokemonClickCallback = (pokemonId: string) => void

/**
 * 槽位点击回调函数类型
 * Slot click callback function type
 * @typedef {(slotIndex: number, position: THREE.Vector3) => void} SlotClickCallback
 */
type SlotClickCallback = (slotIndex: number, position: THREE.Vector3) => void

/**
 * 交互管理 Composable
 * @description 管理 3D 战场中的所有用户交互
 * @param sceneRef - Three.js Scene 的 Ref
 * @param cameraRef - Camera 的 Ref
 * @param domElement - HTML 元素的 Ref
 * @param pokemonEntities - 场上宝可梦引用的 Map
 * @returns 交互相关的状态和方法
 * 
 * @example
 * const {
 *   selectedPokemon,
 *   hoveredPokemon,
 *   clearSelection,
 *   onPokemonClick
 * } = useInteraction(sceneRef, cameraRef, domElement, pokemonEntities)
 */
export function useInteraction(
  sceneRef: Ref<THREE.Scene>,
  cameraRef: Ref<THREE.Camera>,
  domElement: Ref<HTMLElement>,
  pokemonEntities: Ref<Map<string, PokemonEntity>>
) {
  // Raycaster 实例 / Raycaster instance
  const raycaster = new THREE.Raycaster()
  const mouse = new THREE.Vector2()

  // 交互状态 / Interaction state
  const selectedPokemon = ref<string | null>(null)
  const hoveredPokemon = ref<string | null>(null)
  const selectedAction = ref<ActionType>('move')
  const selectedMoveIndex = ref<number>(0)
  const interactionMode = ref<InteractionMode>('select')

  // 回调函数列表 / Callback function lists
  const pokemonClickCallbacks: PokemonClickCallback[] = []
  const slotClickCallbacks: SlotClickCallback[] = []

  // 高亮材质 / Highlight material
  const highlightMaterial = new THREE.MeshStandardMaterial({
    color: 0x00ff00,
    emissive: 0x00ff00,
    emissiveIntensity: 0.3
  })

  // 存储原始材质 / Store original materials
  const originalMaterials = new Map<THREE.Object3D, THREE.Material | THREE.Material[]>()

  /**
   * 更新鼠标坐标
   * Update mouse coordinates
   * @param event - 鼠标事件 / Mouse event
   */
  const updateMousePosition = (event: MouseEvent) => {
    const element = domElement.value
    if (!element) return

    const rect = element.getBoundingClientRect()
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  }

  /**
   * 获取鼠标指向的宝可梦
   * Get pokemon under mouse
   * @returns 宝可梦实体或 null / Pokemon entity or null
   */
  const getPokemonUnderMouse = (): PokemonEntity | null => {
    if (!sceneRef.value || !cameraRef.value) return null

    raycaster.setFromCamera(mouse, cameraRef.value)
    const scene = sceneRef.value

    // 收集所有可交互对象 / Collect all interactive objects
    const interactableObjects: THREE.Object3D[] = []
    pokemonEntities.value.forEach(entity => {
      if (entity.model) {
        interactableObjects.push(entity.model)
      }
    })

    const intersects = raycaster.intersectObjects(interactableObjects, true)

    if (intersects.length > 0) {
      // 找到最近的宝可梦 / Find nearest pokemon
      const hitObject = intersects[0].object
      for (const [id, entity] of pokemonEntities.value.entries()) {
        if (entity.model === hitObject || entity.model.getObjectById(hitObject.id)) {
          return entity
        }
      }
    }

    return null
  }

  /**
   * 高亮宝可梦模型
   * Highlight pokemon model
   * @param pokemon - 宝可梦实体 / Pokemon entity
   */
  const highlightPokemon = (pokemon: PokemonEntity) => {
    if (!pokemon.model) return

    // 保存原始材质 / Save original material
    if (!originalMaterials.has(pokemon.model)) {
      originalMaterials.set(pokemon.model, pokemon.model.material)
    }

    // 应用高亮材质 / Apply highlight material
    pokemon.model.material = highlightMaterial
  }

  /**
   * 移除宝可梦高亮
   * Remove pokemon highlight
   * @param pokemon - 宝可梦实体 / Pokemon entity
   */
  const unhighlightPokemon = (pokemon: PokemonEntity) => {
    if (!pokemon.model) return

    // 恢复原始材质 / Restore original material
    const originalMaterial = originalMaterials.get(pokemon.model)
    if (originalMaterial) {
      pokemon.model.material = originalMaterial
      originalMaterials.delete(pokemon.model)
    }
  }

  /**
   * 处理鼠标移动事件
   * Handle mouse move event
   * @param event - 鼠标事件 / Mouse event
   */
  const handleMouseMove = (event: MouseEvent) => {
    updateMousePosition(event)

    const pokemon = getPokemonUnderMouse()

    // 如果悬停的宝可梦改变 / If hovered pokemon changed
    if (hoveredPokemon.value !== pokemon?.id) {
      // 移除旧的高亮 / Remove old highlight
      if (hoveredPokemon.value) {
        const oldEntity = pokemonEntities.value.get(hoveredPokemon.value)
        if (oldEntity) {
          unhighlightPokemon(oldEntity)
        }
      }

      // 设置新的高亮 / Set new highlight
      if (pokemon) {
        highlightPokemon(pokemon)
        hoveredPokemon.value = pokemon.id
      } else {
        hoveredPokemon.value = null
      }
    }
  }

  /**
   * 处理鼠标点击事件
   * Handle mouse click event
   * @param event - 鼠标事件 / Mouse event
   */
  const handleClick = (event: MouseEvent) => {
    updateMousePosition(event)

    const pokemon = getPokemonUnderMouse()

    if (pokemon) {
      // 点击宝可梦 / Click on pokemon
      if (interactionMode.value === 'select') {
        // 选中/取消选中 / Select/deselect
        if (selectedPokemon.value === pokemon.id) {
          selectedPokemon.value = null
        } else {
          selectedPokemon.value = pokemon.id
        }

        // 触发回调 / Trigger callbacks
        pokemonClickCallbacks.forEach(callback => callback(pokemon.id))
      } else if (interactionMode.value === 'target') {
        // 选择目标 / Select target
        selectedPokemon.value = pokemon.id
        interactionMode.value = 'confirm'
      }
    } else {
      // 点击场地位置 / Click on field position
      if (interactionMode.value === 'select' || interactionMode.value === 'target') {
        // 获取场地位置 / Get field position
        raycaster.setFromCamera(mouse, cameraRef.value)
        const groundPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0)
        const intersectPoint = new THREE.Vector3()
        raycaster.ray.intersectPlane(groundPlane, intersectPoint)

        if (intersectPoint) {
          // 计算槽位索引（示例逻辑）/ Calculate slot index (example logic)
          const slotIndex = Math.floor(intersectPoint.x / 2) + 2

          // 触发槽位点击回调 / Trigger slot click callbacks
          slotClickCallbacks.forEach(callback => callback(slotIndex, intersectPoint))
        }
      }
    }
  }

  /**
   * 处理键盘按下事件
   * Handle keydown event
   * @param event - 键盘事件 / Keyboard event
   */
  const handleKeyDown = (event: KeyboardEvent) => {
    switch (event.key) {
      case '1':
      case '2':
      case '3':
      case '4':
        // 选择招式 1-4 / Select move 1-4
        selectedMoveIndex.value = parseInt(event.key) - 1
        selectedAction.value = 'move'
        console.log(`Selected move index: ${selectedMoveIndex.value}`)
        break

      case 'q':
      case 'Q':
        // 切换到换人模式 / Switch to switch mode
        selectedAction.value = 'switch'
        interactionMode.value = 'select'
        console.log('Switch mode activated')
        break

      case 'e':
      case 'E':
        // 使用特殊系统（太晶化等）/ Use special system (Terastallize, etc.)
        selectedAction.value = 'special'
        console.log('Special system activated')
        break

      case ' ':
        // 确认/提交 / Confirm/submit
        if (interactionMode.value === 'confirm') {
          console.log('Action confirmed:', {
            action: selectedAction.value,
            moveIndex: selectedMoveIndex.value,
            target: selectedPokemon.value
          })
          // 这里可以触发提交逻辑 / Trigger submission logic here
          clearSelection()
        }
        event.preventDefault()
        break

      case 'Escape':
        // 取消/返回 / Cancel/return
        clearSelection()
        console.log('Selection cleared')
        break

      default:
        break
    }
  }

  /**
   * 清除所有选择
   * Clear all selections
   */
  const clearSelection = () => {
    // 移除高亮 / Remove highlight
    if (selectedPokemon.value) {
      const entity = pokemonEntities.value.get(selectedPokemon.value)
      if (entity) {
        unhighlightPokemon(entity)
      }
    }
    if (hoveredPokemon.value) {
      const entity = pokemonEntities.value.get(hoveredPokemon.value)
      if (entity) {
        unhighlightPokemon(entity)
      }
    }

    selectedPokemon.value = null
    hoveredPokemon.value = null
    selectedAction.value = 'move'
    selectedMoveIndex.value = 0
    interactionMode.value = 'select'
  }

  /**
   * 注册宝可梦点击回调
   * Register pokemon click callback
   * @param callback - 回调函数 / Callback function
   * @returns 取消注册函数 / Unregister function
   */
  const onPokemonClick = (callback: PokemonClickCallback) => {
    pokemonClickCallbacks.push(callback)
    return () => {
      const index = pokemonClickCallbacks.indexOf(callback)
      if (index > -1) {
        pokemonClickCallbacks.splice(index, 1)
      }
    }
  }

  /**
   * 注册槽位点击回调
   * Register slot click callback
   * @param callback - 回调函数 / Callback function
   * @returns 取消注册函数 / Unregister function
   */
  const onSlotClick = (callback: SlotClickCallback) => {
    slotClickCallbacks.push(callback)
    return () => {
      const index = slotClickCallbacks.indexOf(callback)
      if (index > -1) {
        slotClickCallbacks.splice(index, 1)
      }
    }
  }

  /**
   * 设置交互模式
   * Set interaction mode
   * @param mode - 交互模式 / Interaction mode
   */
  const setInteractionMode = (mode: InteractionMode) => {
    interactionMode.value = mode
  }

  // 生命周期钩子 / Lifecycle hooks
  onMounted(() => {
    const element = domElement.value
    if (element) {
      element.addEventListener('mousemove', handleMouseMove)
      element.addEventListener('click', handleClick)
      window.addEventListener('keydown', handleKeyDown)
    }
  })

  onUnmounted(() => {
    const element = domElement.value
    if (element) {
      element.removeEventListener('mousemove', handleMouseMove)
      element.removeEventListener('click', handleClick)
      window.removeEventListener('keydown', handleKeyDown)
    }

    // 清理材质 / Clean up materials
    originalMaterials.clear()
  })

  return {
    // 状态 / State
    selectedPokemon,
    hoveredPokemon,
    selectedAction,
    selectedMoveIndex,
    interactionMode,

    // 方法 / Methods
    clearSelection,
    onPokemonClick,
    onSlotClick,
    setInteractionMode
  }
}

/**
 * 使用示例 / Usage Example
 * 
 * ```typescript
 * import { ref, onMounted } from 'vue'
 * import * as THREE from 'three'
 * import { useInteraction } from './useInteraction'
 * 
 * // 在组件中使用 / Use in component
 * const sceneRef = ref<THREE.Scene>(new THREE.Scene())
 * const cameraRef = ref<THREE.Camera>(new THREE.PerspectiveCamera())
 * const domElement = ref<HTMLElement>(document.getElementById('canvas')!)
 * const pokemonEntities = ref<Map<string, PokemonEntity>>(new Map())
 * 
 * const {
 *   selectedPokemon,
 *   hoveredPokemon,
 *   selectedAction,
 *   selectedMoveIndex,
 *   interactionMode,
 *   clearSelection,
 *   onPokemonClick,
 *   onSlotClick
 * } = useInteraction(sceneRef, cameraRef, domElement, pokemonEntities)
 * 
 * // 注册回调 / Register callbacks
 * onPokemonClick((pokemonId) => {
 *   console.log('Pokemon clicked:', pokemonId)
 * })
 * 
 * onSlotClick((slotIndex, position) => {
 *   console.log('Slot clicked:', slotIndex, position)
 * })
 * ```
 */