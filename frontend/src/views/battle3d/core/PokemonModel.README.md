# PokemonModel.ts

宝可梦 3D 模型管理模块，负责创建和管理战场上的宝可梦实体。

## 功能特性

### 1. 模型设计（几何体组合占位）
- **身体**：胶囊几何体，颜色基于宝可梦属性类型
- **头部**：球体，与身体相连
- **眼睛**：白色球体 + 黑色瞳孔
- **属性指示**：身体上方的彩色光环，颜色对应属性

### 2. 属性颜色映射
支持 18 种宝可梦属性类型：
- 普通、火、水、电、草、冰、格斗、毒
- 地面、飞行、超能、虫、岩石、幽灵、龙、恶、钢、妖精

### 3. 状态动画
- **idle**：上下浮动 + 微旋转
- **attack**：向前冲刺 + 缩放
- **hit**：抖动 + 闪红
- **faint**：旋转倒下 + 透明度降低
- **heal**：绿色光晕脉冲

### 4. 主要方法
- `setPosition(x, y, z)`: 设置位置
- `playAnimation(type, duration)`: 播放动画
- `updateHpBar(currentHp, maxHp)`: 更新头顶血条
- `setNameTag(name)`: 设置名字标签
- `setHighlighted(bool)`: 选中高亮效果
- `dispose()`: 清理资源

## 使用示例

### 基本使用
```typescript
import { PokemonEntity } from './PokemonModel'

// 创建宝可梦实体
const pikachu = new PokemonEntity({
  name: '皮卡丘',
  type: '电',
  currentHp: 100,
  maxHp: 100
})

// 设置位置
pikachu.setPosition(0, 0, 0)

// 添加到场景
scene.add(pikachu.group)

// 播放攻击动画
pikachu.playAnimation('attack', 1000)

// 更新生命值
pikachu.updateHpBar(80, 100)

// 设置高亮
pikachu.setHighlighted(true)

// 清理资源
pikachu.dispose()
```

### 与战斗系统集成
```typescript
import { PokemonBattleManager } from './PokemonModel.integration'

// 创建战斗管理器
const battleManager = new PokemonBattleManager(scene)

// 添加宝可梦到战场
battleManager.addPokemon(
  {
    name: '皮卡丘',
    type: '电',
    currentHp: 100,
    maxHp: 100
  },
  'player',
  0
)

// 播放动画
battleManager.playAttackAnimation('player', 0, 1000)
battleManager.playHitAnimation('opponent', 0, 500)

// 更新生命值
battleManager.updatePokemonHP('player', 0, 80, 100)

// 清理
battleManager.dispose()
```

## 文件结构

- `PokemonModel.ts`: 核心模块，包含 PokemonEntity 类
- `PokemonModel.test.ts`: 测试文件
- `PokemonModel.example.ts`: 使用示例
- `PokemonModel.integration.ts`: 与战斗系统集成示例
- `PokemonModel.README.md`: 本说明文档

## 技术实现

### Three.js 组件
- 使用 `THREE.Group` 组织模型各部分
- 使用 `THREE.Mesh` 创建几何体
- 使用 `THREE.Sprite` 实现血条和名字标签
- 使用 `THREE.CanvasTexture` 动态更新血条

### 动画系统
- 使用 `requestAnimationFrame` 实现动画循环
- 手动实现 TWEEN-like 动画，避免额外依赖
- 支持动画中断和状态重置

### 资源管理
- 自动清理几何体、材质和纹理
- 支持高亮效果的原始材质存储
- 提供完整的 `dispose()` 方法

## 注意事项

1. 初期使用几何体组合代替 glTF 模型
2. 血条和名字标签使用精灵实现，始终面向相机
3. 动画使用手动实现，无需额外依赖
4. 支持 TypeScript 类型检查
5. 兼容现有 Three.js 场景管理

## 依赖

- Three.js (v0.185.1+)
- TypeScript (v5.9.3+)