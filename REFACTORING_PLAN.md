# 后端代码重构计划

## 📋 重构目标

将混乱的后端代码结构重新组织为清晰的模块化架构，提高代码可维护性和可读性。

## 🎯 重构原则

1. **渐进式重构** - 保持服务不中断，逐步迁移
2. **向后兼容** - 保留旧接口，确保现有功能正常
3. **职责单一** - 每个服务只负责一个业务领域
4. **命名统一** - 使用一致的命名规范

## 📊 当前问题

### 服务层问题
- ❌ `PokemonServiceImpl` 和 `PokedexServiceImpl` 功能重叠
- ❌ 19个细粒度服务类，职责不清
- ❌ 缺少按业务领域分组

### 控制器层问题
- ❌ `PokemonController` 和 `PokedexController` 职责不清
- ❌ 缺少按功能分组

### 工具类问题
- ❌ 效果类分散（AbilityEffects、ItemEffects、MoveEffects）
- ❌ 缺少统一的效果计算机制

## 🏗️ 新的架构

### 包结构

```
com.lio9.pokedex
├── service/                    # 服务层（按业务领域分组）
│   ├── pokemon/               # 宝可梦域
│   │   ├── PokemonService.java          # 宝可梦核心服务
│   │   ├── PokemonFormService.java      # 形态服务
│   │   └── impl/
│   │       ├── PokemonServiceImpl.java
│   │       └── PokemonFormServiceImpl.java
│   ├── move/                  # 技能域
│   │   ├── MoveService.java
│   │   └── impl/
│   │       └── MoveServiceImpl.java
│   ├── ability/               # 特性域
│   │   ├── AbilityService.java
│   │   └── impl/
│   │       └── AbilityServiceImpl.java
│   ├── item/                  # 物品域
│   │   ├── ItemService.java
│   │   └── impl/
│   │       └── ItemServiceImpl.java
│   ├── type/                  # 属性域
│   │   ├── TypeService.java
│   │   └── impl/
│   │       └── TypeServiceImpl.java
│   └── calculator/            # 伤害计算域
│       ├── DamageCalculatorService.java
│       └── impl/
│           └── DamageCalculatorServiceImpl.java
├── controller/                # 控制器层（按业务领域分组）
│   ├── pokemon/
│   │   └── PokemonController.java
│   ├── move/
│   │   └── MoveController.java
│   ├── ability/
│   │   └── AbilityController.java
│   ├── item/
│   │   └── ItemController.java
│   └── calculator/
│       └── DamageCalculatorController.java
├── util/                      # 工具类（按功能分组）
│   ├── BattleEffects.java     # 战斗效果（合并特性、道具、技能效果）
│   ├── TypeCalculator.java    # 属性计算
│   └── DamageCalculator.java  # 伤害计算公式
└── config/                    # 配置类
    └── MyBatisConfig.java
```

## 🔄 迁移计划

### 阶段1：创建新服务接口
- [x] 创建新的包结构
- [ ] 创建 `PokemonService.java`
- [ ] 创建 `MoveService.java`
- [ ] 创建 `AbilityService.java`
- [ ] 创建 `ItemService.java`
- [ ] 创建 `TypeService.java`
- [ ] 创建 `DamageCalculatorService.java`

### 阶段2：迁移核心功能
- [ ] 迁移宝可梦核心功能到新 `PokemonService`
- [ ] 迁移技能功能到新 `MoveService`
- [ ] 迁移伤害计算功能到新 `DamageCalculatorService`

### 阶段3：重组控制器
- [ ] 创建领域分组控制器
- [ ] 迁移端点到新控制器
- [ ] 更新路由映射

### 阶段4：重组工具类
- [ ] 合并 `AbilityEffects.java`、`ItemEffects.java`、`MoveEffects.java` 为 `BattleEffects.java`
- [ ] 创建统一的效果计算机制

### 阶段5：清理旧代码
- [ ] 验证新功能正常工作
- [ ] 删除旧的重复服务
- [ ] 清理无用的工具类

## 📝 命名规范

### 服务层
- 接口：`{Domain}Service`
- 实现：`{Domain}ServiceImpl`

### 控制器层
- `{Domain}Controller.java`

### 工具类
- 使用驼峰命名
- 按功能分组

## ⚠️ 注意事项

1. 保持向后兼容
2. 分批测试
3. 备份关键代码
4. 记录迁移历史

## 📅 进度跟踪

- [ ] 阶段1完成
- [ ] 阶段2完成
- [ ] 阶段3完成
- [ ] 阶段4完成
- [ ] 阶段5完成