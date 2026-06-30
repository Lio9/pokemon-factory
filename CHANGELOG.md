# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
