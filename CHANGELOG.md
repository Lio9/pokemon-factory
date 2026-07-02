# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
