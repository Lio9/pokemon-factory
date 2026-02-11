# 宝可梦工厂项目

一个基于Spring Boot + Vue.js的现代化宝可梦图鉴应用，支持完整的宝可梦数据管理和对战功能。

## 🎯 核心功能

- ✅ **完整宝可梦数据** - 支持1350个宝可梦的完整数据（从PokeAPI导入）
- ✅ **对战系统** - 支持个体值、努力值、进化链等对战机制
- ✅ **伤害计算器** - 完整的伤害计算系统，支持物理/特殊攻击、属性相性、暴击计算
- ✅ **异步数据导入** - 支持异步导入和失败重试机制
- ✅ **前端图鉴** - 现代化Vue.js前端应用，支持宝可梦、招式、特性、物品查询
- ✅ **后端API** - 完整的REST API服务
- ✅ **日志系统** - 完整的日志记录，错误信息输出到logs文件夹

## 🚀 快速开始

### 环境要求

- Java 17+
- MySQL 8.0+
- Node.js 16+

### 启动步骤

```bash
# 1. 创建数据库
mysql -u root -p < complete_database_init.sql

# 2. 配置数据库连接
# 编辑 pokemon-factory-backend/pokeDex/src/main/resources/application.yml
# 修改数据库连接信息

# 3. 启动后端服务
cd pokemon-factory-backend/pokeDex
mvn spring-boot:run

# 4. 启动前端应用
cd pokemon-factory-frontend
npm install
npm run dev

# 5. 导入数据（可选）
# 方式1：通过前端导入管理页面（推荐）
# 访问 http://localhost:3000/admin/import
# 或在前端页面快速点击左上角Logo 3次进入导入管理页面

# 方式2：通过API接口
curl -X POST http://localhost:8080/api/import/all
```

### 访问应用

- **前端应用**: http://localhost:3000/
- **伤害计算器**: http://localhost:3000/calculator
- **导入管理页面**: http://localhost:3000/admin/import
- **后端API**: http://localhost:8080/

## 📁 项目结构

```
pokemon-factory/
├── pokemon-factory-backend/     # 后端Spring Boot项目
│   ├── common/                  # 通用模块
│   │   └── src/main/java/com/lio9/common/
│   │       ├── model/           # 实体类
│   │       │   ├── Pokemon.java
│   │       │   ├── Type.java
│   │       │   ├── Move.java
│   │       │   ├── Ability.java
│   │       │   ├── Item.java
│   │       │   ├── EvolutionChain.java
│   │       │   ├── PokemonForm.java
│   │       │   └── ...
│   │       ├── mapper/          # MyBatis映射器
│   │       └── service/         # 服务层
│   └── pokeDex/                 # 主应用模块
│       ├── src/main/java/com/lio9/pokedex/
│       │   ├── controller/      # 控制器
│       │   │   ├── ImportController.java       # 统一导入控制器
│       │   │   ├── PokemonController.java      # 宝可梦控制器
│       │   │   ├── MoveController.java         # 招式控制器
│       │   │   ├── AbilityController.java      # 特性控制器
│       │   │   └── ItemController.java         # 物品控制器
│       │   └── service/         # 服务实现
│       │       └── PokeapiDataService.java     # PokeAPI数据服务
│       ├── src/main/resources/
│       │   ├── application.yml  # 配置文件
│       │   ├── log4j2.xml       # 日志配置
│       │   └── mapper/          # MyBatis映射文件
│       └── logs/                # 日志文件夹（自动创建）
│           ├── data-import.log  # 数据导入日志
│           └── error.log        # 错误日志
├── pokemon-factory-frontend/    # 前端Vue.js项目
│   ├── src/
│   │   ├── components/          # 组件
│   │   │   ├── PokemonList.vue
│   │   │   ├── PokemonDetail.vue
│   │   │   ├── MoveList.vue
│   │   │   ├── AbilityList.vue
│   │   │   ├── ItemList.vue
│   │   │   ├── DamageCalculator.vue      # 伤害计算器组件
│   │   │   └── ImportManager.vue         # 导入管理组件（隐藏）
│   │   ├── router/              # 路由配置
│   │   │   └── index.js         # 包含伤害计算器路由
│   │   ├── services/            # API服务
│   │   │   └── api.js
│   │   └── main.js              # 入口文件
│   └── package.json
├── scripts/                     # 数据处理脚本
│   ├── batch_download.py        # 图片下载脚本
│   ├── verify_paths.py          # 路径验证脚本
│   └── utils.py                 # 工具函数
├── complete_database_init.sql   # 完整数据库初始化脚本
├── README.md                    # 项目说明
└── .gitignore                   # Git忽略文件
```

## 💰 伤害计算器功能

### 功能特性

- **完整伤害计算** - 支持物理/特殊攻击的完整伤害计算
- **属性相性系统** - 完整的属性相性计算（25种属性组合）
- **暴击系统** - 基于宝可梦等级的暴击率计算
- **自定义属性** - 支持手动调整宝可梦属性值
- **实时计算** - 选择技能后实时显示伤害范围
- **详细结果** - 显示基础伤害、随机值、能力修正等计算过程

### 计算参数

- **基础伤害公式** - 基于宝可梦种族值和技能威力
- **随机范围** - 85% - 100%的随机修正
- **STAB加成** - 相同属性技能1.5倍伤害
- **属性相性** - 0.25x、0.5x、1x、2x、4x等相性效果
- **等级修正** - 基于宝可梦等级的伤害调整

### 使用方法

1. 打开伤害计算器页面：`http://localhost:3000/calculator`
2. 选择攻击方和防御方的宝可梦
3. 选择技能并查看计算结果
4. 可自定义宝可梦属性值进行精确计算

## 📊 数据库设计

### 核心表结构

**主表**
- `pokemon` - 宝可梦主表
- `type` - 属性表
- `ability` - 特性表
- `move` - 招式表
- `item` - 物品表
- `egg_group` - 蛋群表
- `growth_rate` - 经验类型表

**宝可梦相关表**
- `pokemon_form` - 宝可梦形态表
- `pokemon_stats` - 种族值表
- `pokemon_iv` - 个体值表
- `pokemon_ev` - 努力值表
- `evolution_chain` - 进化链表

**关联表**
- `pokemon_form_type` - 宝可梦形态与属性的关联表
- `pokemon_form_ability` - 宝可梦形态与特性的关联表
- `pokemon_move` - 宝可梦与招式的关联表
- `pokemon_egg_group` - 宝可梦与蛋群的关联表

### 特色功能

- **个体值系统** - 0-31的随机值，影响最终能力值
- **努力值系统** - 0-252的训练值，可自定义培养方向
- **完整进化链** - 支持多级进化和复杂进化条件
- **种族值系统** - 形态独立的HP、攻击、防御等属性

## 🌐 API接口

### 数据导入接口

- `POST /api/import/all` - 统一导入所有数据（异步）
- `GET /api/import/status/{taskId}` - 查询导入任务状态
- `GET /api/import/tasks` - 获取所有导入任务列表
- `POST /api/import/pokemon/range` - 导入指定范围的宝可梦数据（异步）
- `DELETE /api/import/all` - 清空所有数据

### 宝可梦数据接口

- `GET /api/pokemon/list` - 获取宝可梦列表（支持分页和搜索）
- `GET /api/pokemon/{id}` - 获取宝可梦详情
- `GET /api/pokemon/search` - 搜索宝可梦
- `GET /api/pokemon/number/{indexNumber}` - 根据编号获取宝可梦

### 招式数据接口

- `GET /api/moves/list` - 获取招式列表
- `GET /api/moves/{id}` - 获取招式详情
- `GET /api/moves/search` - 搜索招式

### 特性数据接口

- `GET /api/abilities/list` - 获取特性列表
- `GET /api/abilities/{id}` - 获取特性详情
- `GET /api/abilities/search` - 搜索特性

### 物品数据接口

- `GET /api/items/list` - 获取物品列表
- `GET /api/items/{id}` - 获取物品详情
- `GET /api/items/search` - 搜索物品

### 伤害计算器接口

- `GET /api/pokemon/{id}/moves` - 获取宝可梦技能列表（用于伤害计算器）

## 📥 数据导入说明

### 导入流程

1. **清空数据** - 自动清空所有相关数据表（17个表）
2. **导入基础数据** - 类型、蛋群、经验类型
3. **导入特性数据** - 从PokeAPI导入350个特性
4. **导入技能数据** - 从PokeAPI导入1000个技能
5. **导入物品数据** - 从PokeAPI导入2500个物品
6. **导入宝可梦数据** - 从PokeAPI导入1350个宝可梦
7. **失败重试** - 自动重试所有失败的导入记录
8. **完成统计** - 输出最终导入统计结果

### 数据来源

所有数据均从[PokeAPI](https://pokeapi.co/)获取，包括：
- 宝可梦基本信息、形态、属性、特性、技能、种族值
- 进化链信息
- 技能的属性、分类、威力、命中、PP值等
- 特性的描述和效果
- 物品的分类、价格、效果等

### 日志记录

- **数据导入日志**: `pokemon-factory-backend/pokeDex/logs/data-import.log`
- **错误日志**: `pokemon-factory-backend/pokeDex/logs/error.log`
- 日志文件会按日期滚动，最多保留10个历史文件

### 导入特性

- ✅ 异步导入，不阻塞主线程
- ✅ 失败记录和自动重试机制
- ✅ 实时进度跟踪
- ✅ 完整的日志记录
- ✅ 支持中文、英文、日文数据
- ✅ 每次导入前自动清空旧数据

## 🎨 前端功能

### 主要页面

- **宝可梦列表** - `/pokemon` - 查看所有宝可梦，支持搜索和分页
- **宝可梦详情** - `/pokemon/:id` - 查看宝可梦详细信息
- **招式列表** - `/moves` - 查看所有招式
- **特性列表** - `/abilities` - 查看所有特性
- **物品列表** - `/items` - 查看所有物品
- **伤害计算器** - `/calculator` - 完整的伤害计算系统
- **导入管理** - `/admin/import` - 数据导入管理页面（隐藏入口）

### 隐藏功能

- **导入管理页面**：
  - 访问方式1：快速点击页面左上角Logo 3次
  - 访问方式2：直接访问 `/admin/import`
  - 功能：启动全量导入、清空数据、查看导入进度、查看任务历史

## 🛠️ 开发指南

### 后端开发

```bash
cd pokemon-factory-backend
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包部署
mvn package
```

### 前端开发

```bash
cd pokemon-factory-frontend
# 开发模式
npm run dev

# 构建生产版本
npm run build

# 预览生产版本
npm run preview
```

## 🚀 部署说明

### 生产环境部署

1. **后端部署**
```bash
# 构建jar包
cd pokemon-factory-backend
mvn clean package -DskipTests

# 运行
java -jar pokeDex/target/pokeDex-0.0.1-SNAPSHOT.jar
```

2. **前端部署**
```bash
# 构建静态文件
cd pokemon-factory-frontend
npm run build

# 将dist目录部署到Nginx或其它Web服务器
```

3. **配置生产环境**
- 修改数据库连接信息
- 配置日志输出路径
- 设置合适的JVM参数

## 📚 技术栈

### 后端技术栈
- Spring Boot 3.3.5
- Java 17
- MyBatis Plus 3.5.5
- MySQL 9.5.0
- Maven
- Log4j2

### 前端技术栈
- Vue 3.2.13
- Vite 4.0
- Element Plus 2.2.0
- Tailwind CSS 3.4.19
- Vue Router 4.0
- Axios
- Lucide Vue Next (图标库)

### 伤害计算器技术实现
- **前端组件化** - 使用Vue 3 Composition API构建
- **实时计算** - 响应式数据绑定实现即时计算
- **属性系统** - 完整的宝可梦属性和技能数据结构
- **样式设计** - Tailwind CSS实现现代化UI

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进这个项目！

## 📄 许可证

MIT License