# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.3.0] — 2026-08-14

### Added
- **前端设计系统统一**: 新增 `CatalogPageHeader` 组件，图鉴/技能/特性/物品四页统一渐变头部；Element Plus 全局品牌化（poke-red 主色、统一圆角）；补全 `glass-card` 毛玻璃与 `btn-primary` 按钮全局类
- **对战 Showdown 化**: 键盘操作（数字键选招、Enter 提交、S 切换招式/换人、R 刷新）、招式按钮 Showdown 风格编号与 PP 显示、回合日志行动摘要栏（出手顺序+伤害）、目标选择支持点击场上精灵（带精灵图与 HP）、当前回合高亮与精灵入场动画
- **伤害计算器增强**: 表单状态 localStorage 持久化（刷新恢复配置）、头部品牌化、游戏风计算按钮
- **数据修复脚本**: `scripts/fix_move_data.py`（从本地缓存重放招式字段）、`scripts/rebuild_move_table.py`（完全重建 move 表）

### Fixed
- **重大：类型编号错位**: `PokemonType` 枚举与 `DamageCalculatorUtil.TYPE_*` 使用游戏内编号（fire=2），与数据库 PokeAPI 编号（fire=10）不一致，导致天气加成、树果抗性、蓄水/引火等大量机制失效——已统一为 PokeAPI 编号
- **重大：move 表数据错乱**: 离线种子 MOVES 元组字段顺序与 INSERT 列顺序不匹配，导致全部招式 type_id/power/pp/accuracy 错位（tackle 的 type_id=3 应为 1、power=100 应为 40），直接破坏对战引擎计算——已从 947 个 PokeAPI 缓存文件完全重建
- **Jaboca/Rowap 树果判定反了**: 嘉宝果应物理招反伤、罗子果应特殊招反伤，原代码用反了伤害分类常量
- **顺风回合数**: 3 → 4（正作规则）
- **Struggle 属性**: 改为无属性（type_id=0），不再被幽灵系免疫
- **极巨化换人漏洞**: 极巨化期间主动换人被拦截（正作规则）
- **AI 选招增强**: 加入伤害估算（STAB × 属性克制 × 基础伤害，Showdown 风格），不再按招式顺序盲选
- **游客模式错误横幅**: `useBattlePageState` 的 Proxy 不再静默回退到需 JWT 的接口，游客调用 profile/factoryStatus 时静默返回 null
- **轮询竞态**: silent 轮询在 busyAction 进行中跳过，防止陈旧响应回滚 UI
- **交换弹窗重复弹出**: 交换确认后标记 `exchangeJustConfirmed`，防止 applyBattlePayload 再次自动弹出
- **触屏设备按钮不可见**: 详情按钮从 `opacity-0 group-hover:opacity-100` 改为 `opacity-70 group-hover:opacity-100`
- **前端类型色错位**: MoveButton/DamageCalculator 的类型颜色表同步为 PokeAPI 编号
- **详情页浮动栏遮挡导航**: 改为底部悬浮球组（移动端）/ 顶部导航下方（桌面端）
- **死类清理**: `w-4.5/h-4.5` → `w-5/h-5`

---

## [2.2.0] — 2026-07-01

### Added
- **PokeAPI v2 数据初始化脚本**: `init_data.py` 支持 `--online` 从 PokeAPI JSON 下载完整数据（含 zh-Hans 中文名），支持本地缓存断点续传
- **数据完整性验证脚本**: 校验 52 张表的行数和中文名覆盖率
- **中文名补充**: 离线种子数据包含 34 特性/46 道具/77 技能的中文名

### Changed
- **主入口切换**: 从 `one-server`（8081）迁移到 `BattleFactoryApplication`（8084），Vite 代理同步更新
- **数据库路径解析**: `BattleFactoryDataSourceConfig` 优先查找 `backend/pokemon-factory.db`，修复因根目录存有旧数据库导致的连接错误
- **宝可梦详情页 500 修复**: `PokemonEggGroup` 字段 `pokemonId` 改为 `speciesId` 匹配数据库列名
- **属性相克 API**: 前端路径 `/api/damage/types/efficacy` → `/api/damage/type-efficacy`，`normalizeServerData` 兼容嵌套 Map 格式
- **Sprite URL 修复**: `battle/application.yml` 补全 `image-base-url` 配置项，修复精灵图显示为 null 的问题
- **可学习技能模块实现**: `getFormMoves()` 从占位实现改为查询 `pokemon_form_move` 表，同步修复 SQLite 不支持的 `GROUP_CONCAT...SEPARATOR` 语法
- **API 智能路由修复**: `useBattlePageState` 的 `bat` 代理增加方法存在性检查，未登录时调用 `factoryStart/profile` 等接口不会崩溃

### Fixed
- **pollingActive 初始化顺序**: 修复 `useBattlePageState` 中 `useBattlePolling` 在 `useBattleDerivedState` 之后调用导致的 TDZ 异常
- **首页统计显示横杠**: `Home.vue` 字段名从 `data.pokemon` 改为 `data.pokemonCount` 匹配后端响应格式

### Removed
- `one-server` 模块（不再维护），由 `battle` 模块统一编译

---

## [2.1.0] — 2026-06-30

### Added
- GitHub Actions CI pipeline: build, test, lint, and Docker image push (#13)
- Production docker-compose.yml with resource limits, health checks, logging config
- `.env.example` with documented environment variables
- CHANGELOG.md for release tracking

### Changed
- **httpClient**: added automatic retry (×2 for 5xx/network errors), request timeout (30s default), AbortController support, and in-flight request deduplication
- **DataCache**: enabled IndexedDB persistent cache (was disabled); cache now survives page refresh
- `PokemonList.vue`: migrated from Options API to `<script setup>` Composition API; removed unused imports (Loading, ArrowUp, CircleCheck, ArrowDown); fixed fragile keyboard shortcut cleanup
- `AbilityList.vue`: migrated from Options API to `<script setup>`; fixed critical bug where `useLocale()` was called outside `setup()` at module top level

### Removed
- `pokemon-factory-backend/` — stale directory containing only compiled target/ artifacts with no source code
- `PokemonList.vue.bak` — stale backup file

### Security
- Updated .gitignore to prevent committing JWT key and .env.local files

---

## [2.0.0] — 2026-05-15

### Added
- Performance optimization: data cache (LRU), request dedup, image preload
- Keyboard shortcuts (/, Escape, Alt+Home)
- PWA support with Service Worker
- Virtual scrolling for large lists
- Glassmorphism design system

### Changed
- Comprehensive UI/UX enhancement across all pages
- Backend architecture cleanup

---

## [1.x] — 2026-05

### Added
- Initial release with complete battle engine
- Pokemon dex, moves, abilities, items CRUD
- Gen 1–9 battle mechanics (Mega/Z/Dynamax/Terastal)
- 180+ abilities, 80+ items, weather/terrain system
- Double battle support with AI opponent
- User authentication (JWT)
- SQLite database with MyBatis-Plus
