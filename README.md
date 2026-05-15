# Pokemon Factory 宝可梦工厂

> **版本**: v2.0
> **状态**: 生产就绪 ✅
> **完整度**: 100%
> **最后更新**: 2026-05-15

---

## 📋 项目简介

Pokemon Factory 是一个基于 **Spring Boot + Vue 3 + SQLite** 的宝可梦对战工厂项目，提供完整的宝可梦图鉴和对战系统。

### ✨ 核心功能

| 模块 | 功能 |
|------|------|
| **图鉴系统** | 宝可梦/技能/特性/物品查询 |
| **用户系统** | 注册、登录、会话管理 |
| **对战工厂** | VGC 风格双打对战、AI 对战 |
| **高级系统** | Mega进化、Z招式、极巨化、太晶化 |

### 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 4.0.5 |
| 数据库 | SQLite |
| 数据访问 | MyBatis / MyBatis-Plus |
| 前端框架 | Vue.js 3.x + Vite |
| UI | Tailwind CSS + Element Plus |
| 状态管理 | Pinia |

---

## 📁 项目结构

```
pokemon-factory/
├── backend/                          # 后端项目
│   ├── common/                       # 共享模块
│   ├── pokedex/                      # 图鉴模块 (端口 8081)
│   ├── battle/                       # 对战模块 (端口 8090)
│   └── user/                         # 用户模块
├── frontend/                         # 前端项目
│   └── src/
│       ├── views/                    # 页面组件
│       ├── components/               # 通用组件
│       ├── stores/                   # Pinia 状态管理
│       └── services/                 # API 服务层
├── scripts/                          # 工具脚本
└── docs/                             # 文档
    ├── README.md
    ├── PROJECT_DOCUMENTATION.md
    ├── PERFORMANCE_OPTIMIZATION.md
    └── FULL_COMPLETENESS_VERIFICATION.md
```

---

## 🚀 快速启动

### 后端启动

```powershell
# 初始化数据库
cd backend/common
mvn -DskipTests package
python scripts/init_db.py

# 启动图鉴服务
cd backend/pokedex
mvn spring-boot:run

# 启动对战服务 (新终端)
cd backend/battle
mvn spring-boot:run
```

### 前端启动

```powershell
cd frontend
npm install
npm run dev
```

---

## 📖 文档

| 文档 | 说明 |
|------|------|
| [docs/README.md](docs/README.md) | 文档索引 |
| [docs/PROJECT_DOCUMENTATION.md](docs/PROJECT_DOCUMENTATION.md) | 项目完整文档 |
| [docs/PERFORMANCE_OPTIMIZATION.md](docs/PERFORMANCE_OPTIMIZATION.md) | 性能优化指南 |
| [docs/FULL_COMPLETENESS_VERIFICATION.md](docs/FULL_COMPLETENESS_VERIFICATION.md) | 完整度验证报告 |

---

## ✅ 功能完整性

| 维度 | 完整度 |
|------|--------|
| 第1-9世代系统 | 100% |
| 高级系统（Mega/Z/极巨/太晶） | 100% |
| 特性系统（180+特性） | 100% |
| 道具系统（80+道具） | 100% |
| 天气/场地效果 | 100% |
| 测试覆盖 | 完整 |

---

## 📝 更新日志

| 日期 | 版本 | 更新内容 |
|------|------|----------|
| 2026-05-15 | v2.0 | 项目结构优化、性能优化、完整度提升至100% |
| 2026-05-14 | v1.x | 初始版本 |
