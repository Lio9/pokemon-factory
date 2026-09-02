# 3D 对战模块设计文档

> **版本**: v1.0.0  
> **作者**: MiMo-v2.5-pro  
> **日期**: 2024-12  
> **分支**: `feature/threejs-battle-rewrite`

## 1. 概述

### 1.1 背景

原有对战模块基于前端 DOM 点击交互，玩法单一、表现力弱，缺乏沉浸感和实时反馈。本次重构使用 Three.js 将对战模块升级为具备 3D 场景、实时动作反馈和基本战斗逻辑的交互式游戏。

### 1.2 目标

- 使用 Three.js 构建沉浸式 3D 对战场景
- 实现实时动画反馈和粒子特效
- 保持与现有后端 API 的兼容性
- 模块化设计，便于后续扩展

## 2. 架构设计

### 2.1 目录结构

```
frontend/src/
├── composables/battle3d/
│   ├── useThreeScene.ts      # Three.js 场景管理
│   ├── useBattleEngine.ts    # 战斗引擎桥接层
│   └── useInteraction.ts     # 交互系统
├── views/
│   ├── Battle3D.vue          # 主组件
│   └── battle3d/
│       ├── components/
│       │   ├── DebugPanel.vue      # 调试面板
│       │   └── SettlementModal.vue # 结算弹窗
│       ├── core/
│       │   ├── BattleField.ts      # 战场环境
│       │   ├── PokemonModel.ts     # 宝可梦模型
│       │   ├── BattleStateMachine.ts # 状态机
│       │   ├── CombatSystem.ts     # 战斗系统
│       │   └── EffectsManager.ts   # 特效管理
│       └── utils/
│           └── debug.ts           # 调试工具
```

### 2.2 核心模块职责

#### 2.2.1 useThreeScene.ts

**职责**: 管理 Three.js 场景的完整生命周期

**核心功能**:
- 创建和管理 Scene、Camera、Renderer
- OrbitControls 相机控制
- 窗口自适应 resize 处理
- requestAnimationFrame 渲染循环
- FPS 计数器
- Raycaster 鼠标拾取

**导出接口**:
```typescript
export function useThreeScene(container: Ref<HTMLElement | null>): {
  scene: Ref<THREE.Scene | null>
  camera: Ref<THREE.PerspectiveCamera | null>
  renderer: Ref<THREE.WebGLRenderer | null>
  controls: Ref<OrbitControls | null>
  raycaster: THREE.Raycaster
  fps: Ref<number>
  addToScene: (object: THREE.Object3D) => void
  removeFromScene: (object: THREE.Object3D) => void
  startRenderLoop: () => void
  stopRenderLoop: () => void
  dispose: () => void
  isReady: Ref<boolean>
}
```

#### 2.2.2 BattleField.ts

**职责**: 创建和管理 3D 战场环境

**核心功能**:
- 椭圆形草地平台（20x14 单位）
- 玩家侧/对手侧阵营划分
- 位置标记（每侧 2 个槽位）
- 装饰物（花朵、岩石、草丛）
- 边界围墙

**导出接口**:
```typescript
export class Battlefield {
  constructor(scene: THREE.Scene)
  setPositionMarker(side: 'player' | 'opponent', slot: number, highlighted: boolean): void
  getSlotWorldPosition(side: 'player' | 'opponent', slot: number): THREE.Vector3
  animate(time: number): void
  dispose(): void
}
```

#### 2.2.3 PokemonModel.ts

**职责**: 宝可梦 3D 模型管理

**核心功能**:
- 几何体组合占位模型（身体、头部、眼睛、属性光环）
- 18 种属性颜色映射
- 状态动画（idle、attack、hit、faint、heal）
- 血条和名字标签（Sprite 实现，始终面向相机）

**导出接口**:
```typescript
export interface PokemonConfig {
  name: string
  type: string
  currentHp: number
  maxHp: number
  scale?: number
}

export class PokemonEntity {
  group: THREE.Group
  constructor(config: PokemonConfig)
  setPosition(x: number, y: number, z: number): void
  playAnimation(type: 'idle' | 'attack' | 'hit' | 'faint' | 'heal', duration: number): void
  updateHpBar(currentHp: number, maxHp: number): void
  setNameTag(name: string): void
  setHighlighted(highlighted: boolean): void
  dispose(): void
}
```

#### 2.2.4 BattleStateMachine.ts

**职责**: 管理战斗阶段流转

**战斗阶段**:
| 阶段 | 英文 | 说明 |
|------|------|------|
| idle | Idle | 等待开始 |
| team-preview | Team Preview | 队伍预览 |
| battle-start | Battle Start | 战斗开始动画 |
| action-select | Action Select | 选择行动 |
| action-executing | Action Executing | 执行行动动画 |
| turn-resolving | Turn Resolving | 回合结算 |
| replacement | Replacement | 替补选择 |
| battle-end | Battle End | 战斗结束 |
| victory | Victory | 胜利 |
| defeat | Defeat | 失败 |

**导出接口**:
```typescript
export class BattleStateMachine extends TypedEventEmitter {
  get current(): BattlePhase
  get previous(): BattlePhase | null
  canTransitionTo(target: BattlePhase): boolean
  getAvailableTransitions(): BattlePhase[]
  transition(target: BattlePhase): boolean
  reset(): void
  snapshot(): object
}
```

#### 2.2.5 CombatSystem.ts

**职责**: 处理战斗逻辑

**核心功能**:
- 队伍数据管理
- 伤害计算（含 STAB、属性相性）
- 状态效果（burn、poison、paralysis、sleep）
- 胜负判定

**伤害公式**:
```
damage = ((2 * level / 5 + 2) * power * (atk / def)) / 50 + 2) * STAB * typeEffectiveness * random
```

#### 2.2.6 EffectsManager.ts

**职责**: 粒子特效和动画管理

**支持特效**:
| 特效 | 方法 | 说明 |
|------|------|------|
| 攻击命中 | `attackHit()` | 粒子流 + 扩散爆发 |
| 受击 | `damageHit()` | 闪白/闪红 + 相机抖动 |
| 治愈 | `heal()` | 绿色上升粒子 |
| 状态效果 | `statusEffect()` | burn/poison/paralysis/sleep/freeze |
| 太晶化 | `terastallize()` | 彩虹水晶旋转 |
| 倒下 | `faint()` | 白色粒子消散 |

#### 2.2.7 useBattleEngine.ts

**职责**: 连接 Vue 状态管理与 3D 战斗系统

**核心功能**:
- 从后端 summary 同步宝可梦实体到 3D 场景
- 管理实体生命周期
- 协调动画播放与状态更新

#### 2.2.8 useInteraction.ts

**职责**: 处理 3D 战场中的用户交互

**支持交互**:
- 点击宝可梦模型：选中/取消选中
- 点击场地位置：选择移动目标
- 鼠标悬停：高亮可交互物体
- 键盘快捷键：1-4 选择招式、Q 换人、E 特殊系统、Space 确认、Escape 取消

## 3. 数据流

### 3.1 战斗流程

```
用户点击"开始对战"
    ↓
调用后端 API (battleApi.start)
    ↓
获取 battle summary
    ↓
useBattlePageState 管理状态
    ↓
useBattleEngine 同步到 3D 场景
    ↓
创建 PokemonEntity 实例
    ↓
用户交互（选择招式/目标）
    ↓
调用后端 API (battleApi.move)
    ↓
获取新的 summary
    ↓
播放动画和特效
    ↓
更新 UI 状态
```

### 3.2 状态同步

```typescript
// 后端 summary 数据结构
interface BattleSummary {
  status: 'preview' | 'running' | 'completed'
  phase: 'team-preview' | 'action' | 'replacement'
  winner: 'player' | 'opponent' | null
  currentRound: number
  playerTeam: Pokemon[]
  opponentTeam: Pokemon[]
  playerActiveSlots: number[]
  opponentActiveSlots: number[]
  rounds: Round[]
}
```

## 4. 性能优化策略

### 4.1 渲染优化

- 使用 `requestAnimationFrame` 驱动渲染循环
- 启用阴影映射但限制阴影贴图大小
- 使用 `BufferGeometry` 减少内存占用
- 实现视锥体剔除（Three.js 默认支持）

### 4.2 对象池模式

特效管理器使用对象池复用粒子系统：
```typescript
// 预分配粒子对象
private particlePools: Map<string, THREE.Points[]> = new Map()

// 从池中获取
private getFromPool(type: string): THREE.Points { ... }

// 回收到池
private returnToPool(type: string, particle: THREE.Points) { ... }
```

### 4.3 动画优化

- 使用手动 TWEEN-like 实现，避免额外依赖
- 动画帧复用 requestAnimationFrame
- 只在状态变化时触发动画

## 5. 扩展点

### 5.1 模型扩展

当前使用几何体占位，后续可替换为 glTF 模型：
```typescript
// 预留 glTF 加载接口
async loadGLTFModel(url: string): Promise<THREE.Group> {
  const loader = new GLTFLoader()
  const gltf = await loader.loadAsync(url)
  return gltf.scene
}
```

### 5.2 网络对战

可通过 WebSocket 实现实时对战：
```typescript
// 预留 WebSocket 接口
const ws = new WebSocket('ws://battle-server/game')
ws.onmessage = (event) => {
  const data = JSON.parse(event.data)
  applyBattlePayload(data)
}
```

### 5.3 AI 对手

可在 CombatSystem 中添加 AI 决策逻辑：
```typescript
// 预留 AI 接口
class BattleAI {
  chooseMove(state: BattleState): MoveSelection { ... }
  chooseTarget(state: BattleState): number { ... }
}
```

## 6. 已知限制

1. **模型精度**: 当前使用几何体占位，视觉效果有限
2. **浏览器兼容**: 依赖 WebGL，不支持旧版浏览器
3. **移动端**: 触摸交互需要特殊处理
4. **音效**: 仅预留接口，未实现完整音效系统

## 7. 测试说明

### 7.1 启动测试

```bash
# 安装依赖
cd frontend && npm install

# 启动开发服务器
npm run dev

# 访问 3D 对战页面
# http://localhost:5173/battle3d
```

### 7.2 功能测试

1. **场景加载**: 页面加载后应显示 3D 战场
2. **相机控制**: 鼠标拖拽旋转、滚轮缩放
3. **开始对战**: 点击"手动对战"按钮
4. **招式选择**: 点击招式按钮
5. **目标选择**: 点击对手宝可梦
6. **提交回合**: 点击"提交回合"按钮
7. **动画反馈**: 攻击、受击、治愈动画
8. **调试面板**: 点击右上角"调试"按钮

### 7.3 性能测试

- 目标帧率: 60 FPS
- 使用调试面板监控 FPS 和内存使用
- 检查特效数量和绘制调用

## 8. 后续计划

### 8.1 短期（1-2 周）

- [ ] 添加更多宝可梦模型（glTF）
- [ ] 实现完整音效系统
- [ ] 优化移动端触摸交互
- [ ] 添加更多粒子特效

### 8.2 中期（1-2 月）

- [ ] 实现网络多人对战
- [ ] 添加 AI 对手系统
- [ ] 支持更多战斗格式
- [ ] 添加回放功能

### 8.3 长期（3-6 月）

- [ ] 完整的 3D 宝可梦模型库
- [ ] 高级光照和后处理效果
- [ ] VR/AR 支持
- [ ] 跨平台优化

---

**文档维护**: 如有疑问请联系前端团队
