# 项目完成状态

更新日期：2026-05-07

## 全项目测试

| 模块 | 测试数 | 结果 |
|------|--------|------|
| common | 11 | ✅ 全部通过 |
| user-module | 5 | ✅ 全部通过 |
| poke-dex | 4 | ✅ 全部通过 |
| battle-factory | 446 | ✅ 全部通过 |
| **合计** | **466** | **✅ 全通过** |

## Showdown 对齐进度

| 模块 | 完成度 | 备注 |
|------|--------|------|
| 核心对战链路 | 100% | 顺序/伤害/命中/状态门禁 |
| Entry Hazards | 100% | 全 4 钉 + 清除 + 免疫 |
| 通用状态 | 98% | 五异常 + 多数交互 + Heal Bell/Refresh/Nightmare/Dream Eater/Sleep Talk + Worry Seed/Gastro Acid/Heart Swap/Psycho Shift |
| Move Registry | 97% | 85+ 分类集 |
| 场地/天气 | 95% | 4天+4地 + Rising Voltage/Expanding Force + Cloud Nine/Air Lock |
| 切换机制 | 95% | 捕获特性 + 招式 + Shed Shell |
| 挥发状态 | 98% | Throat Chop/Smack Down/Octolock/Jaw Lock/No Retreat/Tar Shot + 原有 7 种 |
| 保护招式 | 92% | 6 种保护 + Unseen Fist + 变种接触 |
| 特殊系统 | 85% | Mega/Z/Dynamax/Tera + Z-Status + G-Max |
| 特性 | 88% | ~138 个有逻辑，VGC 核心覆盖 + Huge Power/Gorilla Tactics/Wind Power/Cloud Nine/Gluttony/Infiltrator/Steadfast |
| 道具 | 87% | ~90 个有逻辑，VGC 核心覆盖 + Lansat Berry + 语义修正(heat-rock/black-sludge) |
| **VGC 综合** | **~95%** | 主流对战可跑通 |

## 已完成功能清单

### 对战核心
- ✅ P1 行为偏差修复 (Phase 0)
- ✅ 挥发状态 7 种全链路 (Phase A)
- ✅ 关键道具补齐 (Phase B)
- ✅ Gen8/9 VGC 特性补齐 (Phase C)
- ✅ 招式分类重构 (Phase E)
- ✅ 捕获限制 + 钉子清除 + 保护变种 + 一键启动 (Phase E)
- ✅ Anger Shell / Cud Chew / Z-Status / G-Max / Struggle 反伤 (Phase F)
- ✅ Rising Voltage / Expanding Force / Grassy Glide (Phase F)
- ✅ Mean Look / Block + Shed Shell (Phase F)
- ✅ Costar / Adrenaline Orb / Binding 束缚招式 (Final)
- ✅ Protosynthesis/Quark Drive 完整版 (Final)
- ✅ Blunder Policy / Covert Cloak / Room Service (Final)
- ✅ Orichalcum Pulse / Hadron Engine / Hospitality (Final)
- ✅ Toxic Debris / Seed Sower / Sand Spit / Anger Point (Final)
- ✅ Cell Battery / Snowball / Luminous Moss (Final)
- ✅ Screen Cleaner / Supersweet Syrup / Moody (Final)
- ✅ Ice Face / Disguise / Embody Aspect (Final)
- ✅ Mycelium Might / Protean / Libero (Final)
- ✅ Commander / Trick / Switcheroo (Final)
- ✅ G-Max Wildfire / Cannonade / Vine Lash (Final)
- ✅ Substitute 挡状态 (Final)
- ✅ King's Rock / Razor Fang 每段攻击独立畏缩 (Phase 2 Week 5)
- ✅ Stench 每段攻击独立畏缩 (Phase 2 Week 5)
- ✅ Shell Bell / Life Orb / PP 消费 — 已确认符合 Showdown 行为 (Phase 2 Week 5)
- ✅ Mirror Armor 反弹能力下降 (Phase 2)
- ✅ Endeavor 垂死挣扎 (Phase 2)
- ✅ Photon Geyser / Moongeist Beam 无视特性 (Phase 2)
- ✅ Judgment / Multi-Attack 根据携带道具改变属性 (Phase 2)
- ✅ Last Resort 最终手段 (Phase 2)
- ✅ Protective Pads 保护垫 — 阻挡接触效果 (Phase 3)
- ✅ Mirror Herb 模仿香草 — 复制对方能力提升 (Phase 3)
- ✅ Jaboca Berry / Rowap Berry / Enigma Berry — 受击触发果实 (Phase 3)
- ✅ Micle Berry — 残血时下一招命中率 +1 阶级 (Phase 3)
- ✅ Lax Incense — 降低对手命中率 0.9 倍 (Phase 3)
- ✅ Zoom Lens — 比目标慢时命中率 ×1.2 (Phase 3)
- ✅ Heal Bell / Aromatherapy — 治愈全队异常状态 (Phase 4)
- ✅ Refresh — 治愈自身中毒/灼伤/麻痹 (Phase 4)
- ✅ Nightmare — 恶梦 volatile + 回合末睡眠伤害 (Phase 4)
- ✅ Dream Eater — 食梦，睡眠目标伤害+吸血 (Phase 4)
- ✅ Sleep Talk / Snore — 睡眠中可用招式 (Phase 4)
- ✅ Quick Claw / Quick Draw / Custap Berry / Stall / Lagging Tail / Full Incense — 先制度系统完善 (Phase 2 Week 6)
- ✅ Throat Chop — 喉斩沉默声系招式 (Phase 4.1)
- ✅ Smack Down — 击落使目标地面化 (Phase 4.1)
- ✅ Octolock — 八爪束缚每回合双防 -1 (Phase 4.1)
- ✅ Jaw Lock — 大嚼大嚼束缚双方 (Phase 4.1)
- ✅ No Retreat — 背水一战全能力 +1 自身束缚 (Phase 4.1)
- ✅ Tar Shot — 焦油覆盖火系弱点 ×2 + 速度 -1 (Phase 4.1)
- ✅ Worry Seed — 精神种子变更为不眠特性 (Phase 4.2)
- ✅ Gastro Acid — 胃酸抑制目标特性 (Phase 4.2)
- ✅ Heart Swap — 心灵互换交换能力阶级 (Phase 4.2)
- ✅ Psycho Shift — 精神转移自身异常状态 (Phase 4.2)
- ✅ heat-rock/black-sludge 语义修正 — 不再误作为属性增伤道具 (Phase 5)
- ✅ Lansat Berry — 兰萨果，HP<25%时会心一击率+2阶级 (Phase 5)
- ✅ Gorilla Tactics — 大猩猩战术，物攻x1.5+锁定招式 (Phase 5)
- ✅ Cloud Nine / Air Lock — 天气抑制特性 (Phase 5)
- ✅ Wind Power — 风力发电，受风系招式/顺风进入充电状态，电系伤害x2 (Phase 5)
- ✅ Huge Power / Pure Power — 物攻x2，大力士/瑜伽之力 (Phase 5)
- ✅ Gluttony — 贪吃特性，HP<50%触发树果 (Phase 5)
- ✅ Infiltrator — 穿透特性，无视反射壁/光墙/神秘守护 (Phase 5)
- ✅ Steadfast — 不屈之心，畏缩时速度+1 (Phase 5)

### 项目基础
- ✅ Docker 三服务编排 (common-init + pokedex + battlefactory)
- ✅ nginx 生产代理
- ✅ Vite 开发代理修复
- ✅ API 路由修正 (battleApiBase 独立配置)
- ✅ actuator/shutdown 启用
- ✅ 一键启动 (python start-backend.py --init)
- ✅ docker-compose 配置修正

## 使用方式

```bash
# 首次启动（建表 + 导入 CSV 数据）
python scripts/start-backend.py --init

# 后续启动
python scripts/start-backend.py

# Docker 部署
docker compose -f docker-compose.local.yml up -d

# 全项目测试
cd pokemon-factory-backend && mvn test
```
