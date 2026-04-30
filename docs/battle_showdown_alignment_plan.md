# 对战引擎 Showdown 对齐进度

更新日期：2026-04-30 | 版本：2.0

## 测试指标

| 项目 | 结果 |
|------|------|
| 全项目测试 | 324/324 全通过 ✅ |
| 后端启动 | battle-factory 正常 (8090) / poke-dex 正常 (8081) |
| 一键启动 | `python scripts/start-backend.py --init` |
| API 路由 | 全部正常，需先初始化数据库 |

---

## Showdown 差距全景

**综合完成度：~82%**

| 模块 | 完成度 | 当前覆盖 | 剩余差距 |
|------|--------|----------|----------|
| 核心对战链路 | 100% | 顺序/伤害/命中/门禁 | — |
| Entry Hazards | 100% | 全 4 种入场钉 + 清除 + 免疫 | — |
| 通用状态 | 95% | 五种主要异常全覆盖 | Facade/Hex 等招式级交互 |
| Move Registry | 96% | 80+ 分类集合 | — |
| 场地/天气 | 90% | 4天+4地全实现 | Rising Voltage/Grassy Glide 等缺失 |
| 保护招式 | 85% | Protect/Detect + 4变种 + Unseen Fist | 变种接触特效(Silk Trap降速等) |
| 切换机制 | 90% | 入场钉/特性 + 3种换人限制 | Mean Look/Block 捕获招式 |
| 挥发状态 | 85% | 7种全链路实现 | 替身挡状态/Infatuation交互间隙 |
| 特性 | 75% | ~65 个有逻辑 | Protosynthesis完整版/机会主义等 ~30个 |
| 道具 | 70% | ~55 个有逻辑 | 缺 ~25 个对战道具 |
| 特殊系统 | 70% | Mega/Z/Dynamax/Tera | G-Max 细节、Z-Status效果 |
| 招式分类 | 80% | 7 类移至 MoveRegistry | SOUND/POWDER 等仍混用 |

---

## 已完成工作

### 核心对齐（Phase 0）✅
| 修复项 | 说明 |
|--------|------|
| Intimidate + Defiant/Competitive | 触发链修复 |
| Lightning Rod/Storm Drain | 重定向 + 吸收 + 特攻提升 |
| Heal Block | 阻止所有被动回血 |
| Always-crit moves | `crit_rate` 字段支持 |
| Choice + Taunt | 回退 Struggle |
| White Herb 时序 | 威吓→Defiant→White Herb |

### 挥发状态（Phase A）✅
| 状态 | 实现内容 |
|------|----------|
| Leech Seed | 草系免疫 + 回合末 1/8 吸取 |
| Substitute | 1/4 HP 制造 + 吸收伤害 |
| Perish Song | 3 回合倒计时倒下 |
| Infatuation | 50% 封行动 |
| Curse (Ghost) | 回合末 1/4 扣血 |
| Aqua Ring / Ingrain | 回合末 1/16 回血 |

### 道具补齐（Phase B + E.3）✅
| 道具 | 实现内容 |
|------|----------|
| Heavy-Duty Boots | 免疫所有场地钉 |
| Air Balloon | 地面免疫 + 被击破球 |
| Rocky Helmet | 接触反伤 1/6 |
| Eject Button | 被击中换出 |
| Red Card | 强制换入对手 |

### 特性补齐（Phase C + E.2）✅
| 特性 | 实现内容 |
|------|----------|
| Sharpness | 切割 1.5x |
| Guard Dog | 威吓免疫 + 攻击提升 |
| Purifying Salt | 状态免疫 + 幽灵抗性 0.5x |
| Sword/Beads of Ruin | 防御/特防 25% 削弱 |
| Supreme Overlord | 每倒下队友 +10% |
| Unseen Fist | 接触穿透 Protect |
| Protosynthesis/Quark Drive | Booster Energy 时 1.3x |

### 分类重构（E.1）✅
7 种招式分类从 `name.contains` 硬编码迁至 `MoveRegistry` 统一 Set。

### 边缘机制（E.4 ~ E.7）✅
| 机制 | 实现内容 |
|------|----------|
| 捕获限制 | Arena Trap/Shadow Tag/Magnet Pull |
| 钉子清除 | Rapid Spin/Defog |
| 保护变种 | King's Shield/Obstruct/Silk Trap/Burning Bulwark |
| 一键启动 | `--init` 参数 + actuator/shutdown |

---

## 剩余差距（约 15 天到 99%）

| # | 项目 | 模块 | 预估 | 类型 |
|---|------|------|------|------|
| F.1 | 特性补齐：Protosynthesis完整(依赖晴天)、Opportunist(复制强化)、Cud Chew(二次果)、Anger Shell(怒壳)、Costar(复制 ally 能力) 等 ~15 个 VGC 热门特性 | `BattleDamageSupport` + `BattleConditionSupport` | 3d | 🔴 |
| F.2 | 道具补齐：Heavy-Duty Boots → Rocky Helmet → 已实现；剩余 Eject Button 实际逻辑已加；还缺 Air Balloon pop → 已实现。剩余 Power Herb(一回合蓄力)、Throat Spray(声音招式后提升特攻)、Blunder Policy(miss后速度)等 | `BattleEngine` + `BattleConditionSupport` | 2d | 🟡 |
| F.3 | 招式-场地交互：Rising Voltage(电场地 2x)、Expanding Force(超能场地 2x)、Grassy Glide(草场地先制)、Misty Explosion(薄雾场地自爆)、Earthquake 半伤(草场地) | `BattleDamageSupport` + `BattleConditionSupport` | 2d | 🟡 |
| F.4 | Z-Status 效果：Z-变化招式特有增益（如 Z-睡觉直接满血、Z-强化全属性 +1） | `BattleConditionSupport` | 2d | 🟡 |
| F.5 | G-Max 招式效果：G-Max 专属附加效果（G-Max Wildfire、G-Max Cannonade 等） | `BattleConditionSupport` | 2d | 🟢 |
| F.6 | 招式效果补全：半吸收(Parabolic Charge)、连续招式(Multi-hit moves)、束缚类(Bind/Wrap)、反伤类(Struggle recoil) | `BattleConditionSupport` + `BattleDamageSupport` | 2d | 🟢 |
| F.7 | 捕获招式：Mean Look、Block、Spider Web 等 + Shed Shell 绕过 | `BattleConditionSupport` | 1d | 🟢 |
| F.8 | 基础测试补充：为新 volatile 状态 + 新特性 + 新道具编写测试用例 | `BattleEngineTest` | 2d | 🟡 |

**预计时间：~15 天 → 对齐度 99%+**

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
