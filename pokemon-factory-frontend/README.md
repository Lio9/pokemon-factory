# Pokemon Factory 前端项目

这是一个基于 Vue 3 和 Vite 构建的宝可梦图鉴前端项目，提供宝可梦、技能和特性信息查询功能。

![Vue.js](https://img.shields.io/badge/Vue.js-3.x-green)
![Vite](https://img.shields.io/badge/Vite-4.x-646CFF)
![Element Plus](https://img.shields.io/badge/Element--Plus-2.2.x-blue)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## 功能特点

- 📋 宝可梦列表展示与搜索
- 🔍 技能和特性信息查询
- 📖 详细的宝可梦信息（包括形态、技能、特性、进化链等）
- 🖱️ 友好的用户界面和交互体验
- 📱 响应式设计，支持多种设备

## 技术栈

- [Vue 3](https://v3.vuejs.org/) - 渐进式JavaScript框架
- [Vite](https://vitejs.dev/) - 新一代前端构建工具
- [Element Plus](https://element-plus.org/) - Vue 3 UI组件库
- [Axios](https://axios-http.com/) - Promise-based HTTP客户端
- [Tailwind CSS](https://tailwindcss.com/) - 实用优先的CSS框架

## 项目结构

```
src/
├── components/
│   ├── PokemonList.vue     # 宝可梦列表和详情页面
│   ├── MoveList.vue        # 技能列表和详情页面
│   └── AbilityList.vue     # 特性列表和详情页面
├── App.vue                 # 主应用组件
├── main.js                 # 应用入口文件
└── index.css               # 全局样式文件
```

## 项目设置

### 环境要求

- Node.js >= 16.x
- npm >= 8.x

### 安装依赖

```bash
npm install
```

### 开发环境运行

```bash
npm run dev
```

默认情况下，开发服务器将在 `http://localhost:3000` 上运行。

### 构建生产版本

```bash
npm run build
```

构建后的文件将输出到 `dist` 目录。

### 预览生产构建

```bash
npm run preview
```

## 开发说明

### API代理配置

项目通过Vite的代理功能将API请求转发到后端服务：

- 前端开发服务器运行在 `http://localhost:3000`
- 后端服务需要运行在 `http://localhost:8080`
- 所有以 `/api` 开头的请求都会被代理到后端服务

### 组件功能

#### PokemonList.vue
- 展示宝可梦列表，支持无限滚动加载
- 提供搜索功能
- 显示宝可梦详情（基本信息、形态、技能、特性、进化链）

#### MoveList.vue
- 展示技能列表，支持无限滚动加载
- 提供搜索功能
- 显示技能详情

#### AbilityList.vue
- 展示特性列表，支持无限滚动加载
- 提供搜索功能
- 显示特性详情

## 许可证

本项目采用MIT许可证，详情请见 [LICENSE](LICENSE) 文件。