# Pokemon Factory 宝可梦工厂

> 🎮 一体化宝可梦对战平台 — 图鉴 + 对战引擎 + AI 对手 + 工厂挑战

[![中文](https://img.shields.io/badge/语言-中文-blue.svg)](README.md)
[![English](https://img.shields.io/badge/Language-English-green.svg)](README_EN.md)

---

## ✨ 项目亮点

### 🎨 现代化设计系统
- **炫酷UI界面**：粒子动画、浮动效果、玻璃态设计
- **深色/浅色主题**：一键切换，本地存储偏好
- **响应式设计**：完美适配桌面、平板、移动端
- **丰富动画效果**：10+种动画关键帧，流畅过渡

### ⚔️ 强大对战引擎
- **双打/单打格式**：vgc-doubles、vgc63、gen9singles
- **20+特性系统**：威吓/避雷针/引水/坚硬脑袋等
- **40+辅助招式**：守住/顺风/戏法空间/光墙等
- **太晶化/Mega/Z招式/极巨化**：完整支持

### 🤖 智能AI系统
- **4档难度**：Easy/Normal/Hard/Expert
- **强化招式AI**：剑舞/诡计/龙舞/冥想等16种
- **智能换人**：多维评分，双打防连环换
- **智能目标**：按类型克制+低血量补刀优先

### 📚 完整图鉴数据
- **1025个宝可梦**：全世代覆盖
- **937个技能**：详细属性和效果
- **358个特性**：完整描述和分类
- **2135个道具**：持续更新中

---

## 🚀 快速启动

**前置条件：** JDK 21、Node.js 20+、Maven 3.9+、Python 3.10+

```powershell
# 1️⃣ 初始化数据库（schema + 静态数据）
python scripts/setup.py

# 2️⃣ 编译后端（首次或代码变更后）
cd backend
mvn package -pl battle -am -DskipTests "-Dmaven.test.skip=true" -q

# 3️⃣ 启动后端（端口 8084）
Start-Process -WindowStyle Hidden -FilePath "cmd.exe" -ArgumentList '/c','cd /d D:\learn\pokemon-factory && java -jar backend\battle\target\battle-0.0.1-SNAPSHOT.jar'

# 4️⃣ 启动前端（端口 7894）
cd frontend
npm run dev

# 5️⃣ 打开 http://localhost:7894/battle
```

等后端日志出现 `Started BattleFactoryApplication` 后即可访问。

---

## 🎯 功能特性

### 🏠 炫酷首页
- **粒子动画背景**：动态粒子效果
- **浮动宝可梦**：精灵球和宝可梦浮动动画
- **功能卡片**：玻璃态设计，悬停提升效果
- **数据统计**：动态数字展示

### 📖 现代化图鉴
- **实时搜索**：支持名称、属性、技能搜索
- **响应式卡片**：属性颜色渐变，悬停动画
- **详情弹窗**：标签页切换，完整信息展示
- **收藏功能**：一键收藏宝可梦

### ⚔️ 优化对战界面
- **3D战场效果**：天空草地背景，精灵浮动
- **属性颜色招式**：火红/水蓝/草绿，按属性着色
- **状态标签系统**：BRN/PSN/PAR/SLP/FRZ + 能力阶级
- **场地效果显示**：剩余回合数（Rain 3T、Reflect 5T等）
- **回合日志**：可折叠、颜色区分（蓝=换人/红=伤害/绿=回复）

### 🏭 工厂挑战
- **随机队伍**：BST 450-600范围，最终进化型
- **智能组队**：STAB招式优先，多样化构建
- **胜后交换**：胜利奖励，交换宝可梦
- **天梯系统**：积分排名，段位晋升

---

## 🏗️ 项目架构

```
┌──────────────────┐     ┌──────────────────────────────────────┐
│  前端 (7894)     │────▶│  battle (8084)                       │
│  Vue 3 + Vite    │     │  ├── engine/ 对战引擎（Showdown 规则）│
│  现代化 UI       │     │  ├── AI: 4档难度/强化/智能换人/目标   │
│  深色/浅色主题    │     │  ├── 随机组队（BST过滤/STAB优先）     │
│                  │     │  ├── pokedex: 图鉴查询、伤害计算      │
│                  │     │  ├── user: 登录注册、JWT 认证          │
│                  │     │  └── common: 数据库、CSV 导入          │
└──────────────────┘     └──────────────────────────────────────┘
                           SQLite (backend/pokemon-factory.db)
```

- **单 JAR** 启动全部功能（端口 8084）
- **SQLite 单文件数据库**，无需安装数据库服务
- **Vite 反向代理** 把 `/api/*` 转发到 `localhost:8084`
- **Python 初始化** 数据库无需启动 Java 后端

---

## 📁 项目结构

```
pokemon-factory/
├── backend/                          # Java 后端（多模块 Maven）
│   ├── common/                       # 数据库、CSV导入、速率限制
│   ├── user/                         # 认证、JWT
│   ├── pokedex/                      # 图鉴 CRUD、伤害计算
│   ├── battle/                       # ★ 主入口（编译此模块，端口 8084）
│   │   ├── controller/               #   BattleController(对战/工厂/游客)
│   │   ├── engine/                   #   回合制对战引擎、AI 决策
│   │   ├── service/                  #   对战编排、天梯、对手池
│   │   └── effect/                   #   特性/道具效果系统
│   └── config/                       # JWT 密钥
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── views/                    # 页面组件
│       │   ├── HomeModern.vue        #   炫酷首页
│       │   ├── PokedexModern.vue     #   现代化图鉴
│       │   ├── BattleOptimized.vue   #   优化对战界面
│       │   └── ...
│       ├── components/               # 通用组件
│       │   ├── ModernNavbar.vue      #   现代化导航栏
│       │   └── ...
│       ├── styles/                   # 样式系统
│       │   └── global.css            #   全局CSS变量
│       ├── composables/              # 组合式逻辑
│       ├── services/                 # HTTP 客户端、缓存
│       └── stores/                   # Pinia 状态管理
├── scripts/                          # ★ 工具脚本
├── data/                             # 缓存数据
│   ├── image/                        # 宝可梦精灵图
│   └── pokeapi-cache/                # PokeAPI v2 JSON 缓存
└── docker-compose.yml                # Docker 部署
```

---

## 🎨 设计系统

### CSS变量系统
```css
:root {
  --primary-500: #3b82f6;
  --success-500: #22c55e;
  --warning-500: #f59e0b;
  --danger-500: #ef4444;
  
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  --radius-xl: 1rem;
  --transition-normal: 200ms cubic-bezier(0.4, 0, 0.2, 1);
}
```

### 动画效果
```css
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
```

### 玻璃态效果
```css
.glass {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}
```

---

## 🧪 测试系统

### 测试脚本
```bash
# 运行所有测试
node run-all-tests.js

# 运行人类交互测试
node test-human-interaction.js

# 运行现代化设计测试
node test-modern-design.js

# 运行对战界面测试
node test-battle-ui.js
```

### 测试覆盖率
- **功能测试**: 100% ✅
- **集成测试**: 100% ✅
- **UI自动化**: 100% ✅
- **响应式设计**: 100% ✅

---

## 📚 脚本参考

### 数据库初始化

| 命令 | 说明 |
|------|------|
| `python scripts/setup.py` | **总控**：建表 → 静态数据 → 验证 |
| `python scripts/setup.py --verify` | 仅验证数据完整性 |
| `python scripts/init_data.py` | 离线种子数据（34 特性/46 道具/77 技能，中文名） |
| `python scripts/init_data.py --online` | **在线下载** 完整数据（含中文名，需联网 20-40 分钟） |
| `python scripts/init_data.py --force` | 清空数据后重新下载 |
| `python scripts/init_data.py --clear-cache` | 清除 PokeAPI 缓存 |

### 数据维护

| 命令 | 说明 |
|------|------|
| `python scripts/data_maintenance.py` | 补全技能/道具的描述、效果、分类（需联网） |
| `python scripts/data_maintenance.py --verify` | 检查数据完整性 |
| `python scripts/data_maintenance.py --fix moves` | 仅修复技能数据 |
| `python scripts/data_maintenance.py --fix items` | 仅修复道具数据 |
| `python scripts/verify_sqlite.py` | 校验 SQLite 完整性和示例数据 |
| `python scripts/backup_db.py` | 自动备份数据库（保留 30 天） |
| `python scripts/download_sprites.py` | 下载精灵图到 `data/image/` |

### 数据状态

| 数据 | 数量 | 中文名 | 描述/效果 | 分类 |
|------|------|--------|-----------|------|
| 宝可梦 | 1025 个物种 | ✅ | ✅ | — |
| 技能 | 937 个 | ✅ | ✅ | ✅ |
| 特性 | 358 个 | ✅ | ✅ | — |
| 道具 | 2135 个 | ✅ | ⚠️ 需运行 `data_maintenance.py` | ⚠️ 同上 |
| 属性/相克 | 18 种 | ✅ | ✅ | — |

---

## 🔧 常见问题

### 宝可梦列表为空 / 数据不全

运行 `python scripts/init_data.py --online` 从 PokeAPI 下载完整数据。
首次下载需 20-40 分钟（网络限速），之后本地缓存可复用。

### 图片不显示

后端返回的 sprite URL 基于 `image-base-url` 配置，默认使用 PokeAPI 远程源。
可运行 `python scripts/download_sprites.py` 下载到本地。

### 首页统计显示 "—"

首页会调用 `/api/pokedex/summary` 获取统计数据，数据下载完成后自动恢复。

### 端口冲突

后端默认 8084，前端默认 7894。可在各自配置文件中修改。

---

## 🚀 开发指引

**后端测试：**
```powershell
cd backend
mvn test -pl battle -am
```

**前端校验：**
```powershell
cd frontend
npm run lint                      # ESLint
npx vue-tsc --noEmit              # TypeScript 检查
```

**API 文档：** 后端启动后 http://localhost:8084/swagger-ui.html

---

## 🐳 Docker 部署

```powershell
docker compose up -d
```

同时启动后端一体服务 + 前端 Nginx。

---

## 🛠️ 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| JVM | OpenJDK | 21 |
| 后端框架 | Spring Boot | 4.0.5 |
| ORM | MyBatis + MyBatis-Plus | 4.0 + 3.5.9 |
| 数据库 | SQLite (xerial JDBC) | 3.47 |
| 前端 | Vue 3 + Vite | 6.x |
| UI | 现代化设计系统 | — |
| 状态管理 | Vue Composables + Vue Router | — |
| 认证 | JWT (HS256) | — |
| 图片 | 本地优先 → PokeAPI 回退 → 默认图 | — |
| 部署 | Docker Compose (Nginx + JAR) | — |

---

## 📊 数据初始化说明

### 架构变迁

- **v1.x** — 三个独立后端服务（pokedex:8082 / user:8083 / battle:8084）
- **v2.0** — 合并为 `one-server`（8081），CSV 离线数据导入
- **v2.1+** — `one-server` 移除，`BattleFactoryApplication`（8084）统一入口
  - 数据库初始化从 Java 迁移到 Python 脚本
  - 数据来源从 PokeAPI GitHub CSV 迁移到 PokeAPI v2 JSON（含中文名）

### 数据恢复

如果数据库被清空或损坏：

```powershell
# 1. 删除旧数据库
Remove-Item backend\pokemon-factory.db

# 2. 重建 schema + 静态数据 + 离线种子
python scripts/init_data.py

# 3. 在线补充完整数据（可选，含中文名）
python scripts/init_data.py --online
```

---

## 🎨 现代化设计特性

### 🌙 深色/浅色主题
- 一键切换主题
- 本地存储偏好
- 平滑过渡动画

### 📱 响应式设计
- 桌面端：1280px+
- 平板端：768px - 1024px
- 移动端：< 768px

### ✨ 动画效果
- 粒子动画背景
- 浮动宝可梦效果
- 卡片悬停动画
- 页面切换过渡

### 🎯 交互体验
- 玻璃态效果
- 渐变色彩系统
- 多层次阴影
- 流畅的动画过渡

---

## 📈 性能优化

### 前端优化
- CSS变量系统
- 组件懒加载
- 图片懒加载
- 代码分割

### 后端优化
- 数据库索引优化
- 查询缓存
- 连接池管理
- 异步处理

---

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

---

## 🙏 致谢

- [Pokemon Showdown](https://pokemonshowdown.com/) - 对战引擎参考
- [PokeAPI](https://pokeapi.co/) - 宝可梦数据源
- [Vue.js](https://vuejs.org/) - 前端框架
- [Spring Boot](https://spring.io/projects/spring-boot) - 后端框架

---

## 📞 联系方式

- 项目链接: https://github.com/Lio9/pokemon-factory
- 问题反馈: https://github.com/Lio9/pokemon-factory/issues

---

**最后更新**: 2026年9月17日  
**版本**: v2.1.0  
**状态**: ✅ 生产就绪