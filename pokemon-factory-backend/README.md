# Pokemon Factory Backend

## 📁 项目结构

### 模块划分

```
pokemon-factory-backend/
├── common/                    # 公共模块（数据层）
│   ├── mapper/               # 数据访问层
│   ├── model/                # 实体模型
│   ├── vo/                   # 值对象
│   └── response/             # 通用响应
└── pokeDex/                   # 图鉴模块（业务层）
    ├── service/              # 服务层（按业务领域组织）
    │   ├── pokemon/          # 宝可梦域
    │   │   ├── PokemonService.java          # 接口
    │   │   └── PokemonServiceImpl.java      # 实现
    │   ├── move/             # 技能域
    │   │   ├── MoveService.java
    │   │   └── MoveServiceImpl.java
    │   ├── ability/          # 特性域
    │   │   ├── AbilityService.java
    │   │   └── AbilityServiceImpl.java
    │   ├── item/             # 物品域
    │   │   ├── ItemService.java
    │   │   └── ItemServiceImpl.java
    │   ├── type/             # 属性域
    │   │   ├── TypeService.java
    │   │   └── TypeServiceImpl.java
    │   └── calculator/       # 伤害计算域
    │       ├── DamageCalculatorService.java
    │       ├── EffectCalculatorService.java
    │       └── DamageCalculatorServiceImpl.java
    ├── controller/           # 控制器层（按业务领域组织）
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
    ├── util/                 # 工具类
    │   ├── BattleEffects.java     # 战斗效果
    │   ├── TypeCalculator.java    # 属性计算
    │   └── DamageCalculator.java  # 伤害计算公式
    └── config/               # 配置类
```

### 架构原则

1. **common 模块** - 纯数据层
   - 只包含数据访问和模型定义
   - 不包含业务逻辑
   - 不包含服务接口

2. **pokeDex 模块** - 业务层
   - 按业务领域组织服务
   - 每个领域包含接口和实现
   - 接口和实现在同一包中

3. **控制器层** - API 层
   - 按业务领域分组
   - 与服务层一一对应

### 服务层组织

每个业务领域包包含：
- `{Domain}Service.java` - 服务接口
- `{Domain}ServiceImpl.java` - 服务实现

#### 当前业务领域
1. **pokemon** - 宝可梦相关
2. **move** - 技能相关
3. **ability** - 特性相关
4. **item** - 物品相关
5. **type** - 属性相关
6. **calculator** - 伤害计算相关

### 控制器层组织

每个业务领域包包含：
- `{Domain}Controller.java` - 控制器

## 📝 API 文档

### 宝可梦 API
- `GET /api/pokemon/list` - 获取宝可梦列表
- `GET /api/pokemon/{id}` - 获取宝可梦详情
- `GET /api/pokemon/{id}/moves` - 获取宝可梦技能
- `GET /api/pokemon/{id}/evolution` - 获取进化链

### 技能 API
- `GET /api/moves/list` - 获取技能列表
- `GET /api/moves/{id}` - 获取技能详情

### 伤害计算 API
- `POST /api/damage/calculate` - 计算伤害
- `GET /api/damage/type-efficacy` - 获取属性相性矩阵

## 🚀 运行项目

```bash
# 启动后端服务
cd pokemon-factory-backend/pokeDex
mvn spring-boot:run
```

服务将在 http://localhost:8081 启动

## 📚 技术栈

- Spring Boot 3.3.5
- MyBatis-Plus 3.5.5
- Java 17
- MySQL

## 🔄 重构状态

### 已完成
- ✅ 创建正确的包结构
- ✅ 定义业务领域
- ✅ 创建服务接口

### 进行中
- 🔄 实现服务类
- 🔄 迁移业务逻辑

### 待完成
- ⏳ 重组控制器层
- ⏳ 重组工具类
- ⏳ 清理旧代码