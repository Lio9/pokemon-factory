# 对战引擎 Showdown 对齐计划

更新日期：2026-04-30 | 版本：3.0

## 测试指标

| 项目 | 结果 |
|------|------|
| 全项目测试 | 305/305 全通过 ✅ |
| 后端启动 | battle-factory 正常 (8090) / poke-dex 正常 (8081) |
| 一键启动 | `python scripts/start-backend.py --init` |
| API 路由 | 全部正常，需先初始化数据库 |

---

## Showdown 对齐全景

**综合完成度：~86%**

| 模块 | 完成度 | 已覆盖 | 剩余差距 |
|------|--------|--------|----------|
| 核心对战链路 | 100% | 顺序/伤害/命中/门禁 | — |
| Entry Hazards | 100% | 全 4 钉 + 清除 + 免疫 | — |
| 通用状态 | 95% | 5 种异常全实现 | 招式级交互 |
| Move Registry | 96% | 80+ 分类集 | — |
| 场地/天气 | 91% | 4天+4地 + Rising Voltage/Expanding Force | Grassy Glide 先制、Misty Explosion |
| 切换机制 | 92% | 捕获特性 + 捕获招式 + Shed Shell | — |
| 挥发状态 | 88% | 7 种全链路 | 替身挡状态、Infatuation 残留 |
| 保护招式 | 88% | 6 种保护 + Unseen Fist | 变种接触特效 |
| 招式分类 | 82% | 7 类迁至 MoveRegistry | SOUND/POWDER/BULLET 仍混用 |
| 特性 | 78% | ~68 个有逻辑 | 缺 ~25 个 Gen8/9 特性 |
| 道具 | 73% | ~55 个有逻辑 | 缺 ~20 个对战道具 |
| 特殊系统 | 75% | Mega/Z/Dynamax/Tera + Z-Status | G-Max 效果、Max 招式倍率 |

---

## 已完成工作

### Phase 0 — 核心修复 ✅
Intimidate+Defiant/Lightning Rod/Heal Block/Always-crit/Choice+Taunt/White Herb

### Phase A — 挥发状态 ✅
Leech Seed/Substitute/Perish Song/Infatuation/Curse/Aqua Ring/Ingrain

### Phase B — 道具补齐 ✅
Heavy-Duty Boots/Air Balloon/Rocky Helmet/Eject Button/Red Card/Throat Spray

### Phase C — Gen8/9 特性 ✅
Sharpness/Guard Dog/Purifying Salt/Ruin abilities/Supreme Overlord/Unseen Fist/Protosynthesis

### Phase E — 分类+边缘 ✅
7 类招式分类迁至 MoveRegistry / Eject Button 换人 / Arena Trap / Rapid Spin 清钉 / King's Shield 等保护变种 / 一键启动

### Phase F — 最终补齐 ✅
Anger Shell/Cud Chew / Rising Voltage 2x / Strunggle recoil / Mean Look+Block / Z-Status 效果 / G-Max 注册 / Leech Seed 测试

---

## 剩余差距（约 10 天到 95%+）

| # | 项目 | 模块 | 预估 | 优先级 |
|---|------|------|------|--------|
| G.1 | 特性补齐：Opportunist(复制强化)、Costar(复制 ally 能力)、Protosynthesis 完整版(依赖晴天)、Cud Chew 完整版(回合末再吃)、Sharpness 确认切割招式列表 | `BattleConditionSupport` / `BattleDamageSupport` | 2.5d | 🔴 |
| G.2 | 道具补齐：Power Herb(蓄力跳过)、Blunder Policy(Miss 后速度)、Adrenaline Orb(威吓提速)、Room Service(空间减速)、Eject Pack(降能力换人) | `BattleEngine` / `BattleConditionSupport` | 1.5d | 🟡 |
| G.3 | 招式分类收尾：SOUND/POWDER/BULLET 从 `hasMoveFlag` + 硬编码统一迁至 `MoveRegistry` | `MoveRegistry` / `BattleConditionSupport` | 1d | 🟡 |
| G.4 | 场地交互补充：Grassy Glide 草场地先制、Misty Explosion 薄雾增伤自爆、Psychic Terrain 禁止先制完整版 | `BattleEngine` / `BattleDamageSupport` | 1d | 🟡 |
| G.5 | G-Max 效果：G-Max Wildfire/Cannonade/Vine Lash 持续伤害、G-Max Stonesurge 撒钉等 | `BattleConditionSupport` | 1.5d | 🟢 |
| G.6 | 招式效果补全：Bind/Wrap 束缚回合伤害、Parabolic Charge 半吸收、Multi-hit 连击修正、Throat Spray 喉喷 | `BattleConditionSupport` / `BattleDamageSupport` | 1.5d | 🟢 |
| G.7 | 测试补充：为所有新增 volatile/特性/道具编写回归测试 | `BattleEngineSwitchingTest` | 1d | 🟡 |

**完成 G.1~G.4 = ~6 天 → ~92%**
**全部完成 = ~10 天 → ~95%+**

---

## 使用方式

```bash
# 首次启动（建表 + 导入 CSV 数据）
python scripts/start-backend.py --init

# 后续启动
python scripts/start-backend.py

# 或直接 Maven
POKEMON_FACTORY_DATABASE_INITIALIZE_ON_STARTUP=true \
POKEMON_FACTORY_DATABASE_IMPORT_CSV_ON_STARTUP=true \
  mvn spring-boot:run -pl battle-factory
```
