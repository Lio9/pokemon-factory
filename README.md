# 宝可梦工厂项目

一个基于Spring Boot + Vue.js的现代化宝可梦图鉴应用，支持完整的宝可梦数据管理和对战功能。

## 🎯 核心功能

- ✅ **完整宝可梦数据** - 支持1025个宝可梦的完整数据
- ✅ **对战系统** - 支持个体值、努力值、进化链等对战机制
- ✅ **图片下载** - 从PokeAPI自动下载宝可梦图片
- ✅ **前端图鉴** - 现代化Vue.js前端应用
- ✅ **后端API** - 完整的REST API服务

## 🚀 快速开始

### 环境要求

- Java 17+
- MySQL 8.0+ (当前配置: 192.168.134.129:3306)
- Node.js 16+
- Python 3.8+

### 启动步骤

```bash
# 1. 创建数据库
mysql -u root -p < complete_database_init.sql

# 2. 启动后端服务
cd pokemon-factory-backend/pokeDex
mvn spring-boot:run

# 3. 导入宝可梦数据
curl -X POST http://localhost:8080/api/pokeapi/import-all

# 4. 启动前端应用
cd pokemon-factory-frontend
npm install
npm run dev
```

### 访问应用

- **前端应用**: http://localhost:3000/
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
│   │       │   ├── EvolutionChain.java
│   │       │   ├── PokemonForm.java
│   │       │   └── ...
│   │       ├── mapper/          # MyBatis映射器
│   │       └── service/         # 服务层
│   └── pokeDex/                 # 主应用模块
│       ├── src/main/java/com/lio9/pokedex/
│       │   ├── controller/      # 控制器
│       │   │   ├── PokemonController.java
│       │   │   ├── PokeapiDataController.java
│       │   │   ├── MoveController.java
│       │   │   └── AbilityController.java
│       │   └── service/         # 服务实现
│       └── src/main/resources/
│           ├── application.yml  # 配置文件
│           └── mapper/          # MyBatis映射文件
├── pokemon-factory-frontend/    # 前端Vue.js项目
│   ├── src/
│   │   ├── components/          # 组件
│   │   │   ├── PokemonList.vue
│   │   │   ├── PokemonDetail.vue
│   │   │   ├── MoveList.vue
│   │   │   └── AbilityList.vue
│   │   ├── router/              # 路由配置
│   │   ├── services/            # API服务
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

## 🛠️ 主要脚本

### 图片下载脚本

- `batch_download.py` - 统一的宝可梦图片下载脚本
  ```bash
  # 全量下载（从1到1025）
  python scripts/batch_download.py 1
  
  # 增量下载（指定范围）
  python scripts/batch_download.py 2 1 100
  
  # 验证图片路径
  python scripts/verify_paths.py
  
  # 重新下载缺失图片
  python scripts/batch_download.py 3
  ```

### 数据导入脚本

- `PokeapiDataController.java` - REST API接口
  ```bash
  # 导入所有宝可梦数据
  curl -X POST http://localhost:8080/api/pokeapi/import-all
  
  # 清空所有表数据
  curl -X POST http://localhost:8080/api/pokeapi/clear-all
  
  # 获取导入状态
  curl -X GET http://localhost:8080/api/pokeapi/import-status
  ```

### 数据验证脚本

- `verify_paths.py` - 验证已下载图片的完整性
  ```bash
  python scripts/verify_paths.py
  ```

## 📊 数据库设计

### 核心表结构

- `pokemon` - 宝可梦主表
- `type` - 属性表
- `ability` - 特性表
- `move` - 招式表
- `pokemon_form` - 宝可梦形态表
- `evolution_chain` - 进化链表
- `pokemon_stats` - 种族值表
- `pokemon_iv` - 个体值表
- `pokemon_ev` - 努力值表

### 特色功能

- **个体值系统** - 0-31的随机值，影响最终能力值
- **努力值系统** - 0-252的训练值，可自定义培养方向
- **完整进化链** - 支持多级进化和复杂进化条件
- **种族值系统** - 形态独立的HP、攻击、防御等属性

## 🌐 API接口

### 宝可梦数据接口

- `GET /api/pokemon/list` - 获取宝可梦列表（支持分页和搜索）
- `GET /api/pokemon/{id}` - 获取宝可梦详情
- `GET /api/pokemon/search` - 搜索宝可梦
- `GET /api/pokemon/number/{indexNumber}` - 根据编号获取宝可梦
- `GET /api/pokemon/{id}/evolution` - 获取进化链信息

### 招式数据接口

- `GET /api/moves/list` - 获取招式列表
- `GET /api/moves/{id}` - 获取招式详情
- `GET /api/moves/search` - 搜索招式

### 特性数据接口

- `GET /api/abilities/list` - 获取特性列表
- `GET /api/abilities/{id}` - 获取特性详情
- `GET /api/abilities/search` - 搜索特性

### 数据导入接口

- `POST /api/pokeapi/import-all` - 导入所有宝可梦数据
- `POST /api/pokeapi/clear-all` - 清空所有表数据
- `GET /api/pokeapi/import-status` - 获取导入状态

## 数据库设计

### 主要表结构

- `pokemon` - 宝可梦基础信息表
- `pokemon_form` - 宝可梦形态表
- `type` - 属性表
- `move` - 招式表
- `ability` - 特性表
- `evolution_chain` - 进化链表
- `pokemon_move` - 宝可梦招式关联表

详细的表结构请查看 [database-design.sql](database-design.sql) 文件。

## 开发指南

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

## 部署说明

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

## 📚 详细文档

- [PokeAPI统一下载功能说明](./scripts/PokeAPI统一下载功能说明.md) - 图片下载详细说明

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进这个项目！

## 📄 许可证

MIT License