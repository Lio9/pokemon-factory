# 待补全功能清单

更新日期：2026-05-08（本轮修改）

> 本文档记录尚未实现的功能点，用于后续迭代参考。
> 状态标注：✅ 已实现有逻辑 / ⚠️ 已注册但逻辑不完整 / ❌ 未实现
> 审计范围：EffectRegistry + 引擎代码(硬编码) + MoveRegistry

---

## 一、特性

### P0 - VGC 核心（影响主流对战构筑）

| 特性 | 英文 ID | 状态 | 说明 |
|------|---------|------|------|
| 坚硬岩石 | solid-rock | ✅ Filter/ Solid Rock / Prism Armor 共用 0.75x 方法 |
| 幻影防守 | shadow-shield | ✅ BattleDamageSupport 满 HP 时伤害×0.5 |
| 棱镜装甲 | prism-armor | ✅ regFilterLike 0.75x |
| 唱反调 | contrary | ✅ ContraryAbility 独立类处理能力反转 |
| 不思议鳞粉 | marvel-scale | ✅ EffectRegistry dispatchTargetDefense 异常时物防×1.5 |
| 毛皮大衣 | fur-coat | ✅ EffectRegistry 物伤×0.5 |
| 冰鳞粉 | ice-scales | ✅ EffectRegistry 特伤×0.5 |
| 愤怒穴位 | anger-point | ✅ EffectRegistry onDamageReceived 击中要害攻击+6 |
| 碎裂铠甲 | weak-armor | ✅ EffectRegistry onDamageReceived 物防-1速度+2 |
| 笨拙 | klutz | ✅ EffectRegistry + BattleEngine 全部 item dispatch 检查 Klutz |
| 化学变化气体 | neutralizing-gas | ✅ BattleEngine abilityName(mon, state) 检查场上 Neutralizing Gas |
| 压迫感 | pressure | ✅ hasPressureOnOpposingSide + applyCooldown 额外 PP 消耗 |
| 紧张感 | unnerve | ✅ applyDefenderItemEffects 树果块前检查 Unnerve |
| 熟成 | ripen | ✅ BattleEngine 树果效果翻倍（Sitrus/Pinch/Starf/Resist 全部） |
| 颊囊 | cheek-pouch | ✅ BattleEngine 食用树果后额外回复 1/3 最大 HP |
| 危险预知 | anticipation | ✅ BattleConditionSupport 上场检测对手危险招式 |
| 察觉 | frisk | ✅ BattleConditionSupport 上场查看对手道具 |
| 技术高手 | technician | ✅ EffectRegistry 威力≤60×1.5 |
| 强行 | sheer-force | ✅ EffectRegistry 有追加效果时×1.3 |
| 毅力 | guts | ✅ EffectRegistry 异常时物伤×1.5（灼伤不减物攻由引擎处理） |
| 太阳之力 | solar-power | ✅ EffectRegistry 晴天空特伤×1.5 + 回合末掉血 |
| 耐心 | stall | ✅ BattleEngine Action 必定后手 |
| 毒暴走 | toxic-boost | ✅ EffectRegistry 中毒时物伤×1.5 |
| 全力攻击 | hustle | ✅ EffectRegistry 物伤×1.5 + BattleRoundSupport 命中×0.8 |

### P1 - VGC 有用（特定构筑中重要）

| 特性 | 英文 ID | 状态 | 说明 |
|------|---------|------|------|
| 水泡 | water-bubble | ✅ EffectRegistry 火伤×0.5 + 烧伤免疫 |
| 不仁不义 | merciless | ✅ BattleDamageSupport 攻击中毒目标必中要害 |
| 飞出的内在物 | innards-out | ✅ BattleRoundSupport applyOnKOTargetAbility 倒下时等量伤害 |
| 污泥浆 | liquid-ooze | ✅ BattleConditionSupport 吸取招式回血变伤害 |
| 踩影 | arena-trap | ✅ BattleRoundSupport 对方不能换下 |
| 磁力 | magnet-pull | ✅ 钢系不能换下 |
| 沙穴 | sand-tomb | ❌ 英文 ID 错误，正确为 sand-stream（扬沙），已实现 |
| 吸盘 | suction-cups | ✅ EffectRegistry 注册，引擎防强制替换 |
| 黏着 | sticky-hold | ✅ BattleConditionSupport 防止夺走道具 |
| 贪吃鬼 | gluttony | ✅ BattleEngine HP<50% 触发树果 |
| 黄金之躯 | good-as-gold | ✅ EffectRegistry 免疫变化招式 |
| 洁净之盐 | purifying-salt | ✅ EffectRegistry 免疫变化招式 + 幽灵系伤害×0.5 |
| 流水旋舞 | surf-tail | ❌ 此特性不存在（可能指 swift-swim 悠游自如或 surge-surfer 冲浪之尾） |
| 冲浪之尾 | surge-surfer | ✅ EffectRegistry 电场速度翻倍 |
| 精神力 | inner-focus | ✅ BattleConditionSupport 免疫畏缩 |
| 我行我素 | own-tempo | ✅ EffectRegistry 混乱免疫 |
| 熔岩铠甲 | magma-armor | ✅ EffectRegistry 冰冻免疫 |
| 柔软 | limber | ✅ EffectRegistry 麻痹免疫 |
| 水幕 | water-veil | ✅ EffectRegistry 灼伤免疫 |
| 叶子防守 | leaf-guard | ✅ EffectRegistry 晴天异常免疫 |
| 鲜花帷幕 | flower-veil | ✅ EffectRegistry + 引擎草系队友免疫 |
| 芳香幕 | aroma-veil | ✅ EffectRegistry 精神类免疫（挑衅/着迷等） |
| 甜幕 | sweet-veil | ✅ EffectRegistry 睡眠免疫 + 引擎团队检查 |
| 彩幕 | pastel-veil | ✅ EffectRegistry 中毒免疫 + 引擎团队检查 |
| 奇迹皮肤 | wonder-skin | ✅ BattleRoundSupport 变化招式命中率×0.5 |
| 隔音 | soundproof | ✅ EffectRegistry onTypeImmunity 免疫声系招式 |
| 防弹 | bulletproof | ✅ EffectRegistry onTypeImmunity 免疫球/弹招式 |
| 鳞粉 | shield-dust | ✅ EffectRegistry 阻挡追加效果 |
| 神奇守护 | wonder-guard | ✅ BattleConditionSupport 只有效果绝佳招式命中 |

### P2 - 触发类（条件性有用）

| 特性 | 英文 ID | 状态 | 说明 |
|------|---------|------|------|
| 诅咒之躯 | cursed-body | ✅ BattleConditionSupport 接触 30% 定身法 |
| 孢子 | effect-spore | ✅ EffectRegistry 接触 30% 中毒/麻痹/睡眠 |
| 毒刺 | poison-point | ✅ EffectRegistry 接触 30% 中毒 |
| 火焰之躯 | flame-body | ✅ EffectRegistry 接触 30% 灼伤 |
| 静电 | static | ✅ EffectRegistry 接触 30% 麻痹 |
| 迷人之躯 | cute-charm | ✅ EffectRegistry 接触 30% 魅惑 |
| 黏滑 | gooey | ✅ EffectRegistry 接触速度-1 |
| 卷发 | tangling-hair | ✅ EffectRegistry 接触速度-1 |
| 灭亡之躯 | perish-body | ✅ EffectRegistry 接触双方 3 回合后濒死 |
| 幽香气息 | lingering-aroma | ✅ EffectRegistry 接触变成目标特性 |
| 正义之心 | justified | ✅ EffectRegistry 恶系命中攻击+1 |
| 胆怯 | rattled | ✅ EffectRegistry 恶/虫/幽灵命中速度+1 |
| 蒸汽机 | steam-engine | ✅ EffectRegistry 水/火命中速度+6 |
| 储水 | storm-drain | ✅ EffectRegistry 水免 + 特攻+1 |
| 避雷针 | lightning-rod | ✅ EffectRegistry 电免 + 特攻+1 |
| 引火 | flash-fire | ✅ EffectRegistry 火免 + 特攻+1 |
| 轻装 | unburden | ✅ EffectRegistry 消耗道具后速度翻倍 |
| 飞毛腿 | quick-feet | ✅ EffectRegistry 异常时速度×1.5 |
| 沙隐 | sand-veil | ✅ EffectRegistry + BattleRoundSupport 沙暴闪避×1.25 |
| 雪隐 | snow-cloak | ✅ EffectRegistry + BattleRoundSupport 雪天闪避×1.25 |

### P3 - 低优先级（稀有或机制复杂）

| 特性 | 英文 ID | 状态 | 说明 |
|------|---------|------|------|
| 鱼群 | schooling | ❌ 未实现（弱丁鱼形态变化） |
| 陨石 | shields-down | ❌ 未实现（小陨星形态变化） |
| 战斗切换 | stance-change | ❌ 未实现（坚盾剑怪形态变化） |
| 达摩模式 | zen-mode | ❌ 未实现（达摩狒狒形态变化） |
| 友情防守 | friend-guard | ✅ BattleDamageSupport.partnerAbilityModifier 队友伤害×0.75 |
| 心情不定 | moody | ✅ 引擎已实现 |
| 避雷针（双打） | lightning-rod | ✅ 单双打均实现 |
| 引水（双打） | storm-drain | ✅ 单双打均实现 |
| 缓慢启动 | slow-start | ❌ 未实现 |
| 失败主义 | defeatist | ❌ 未实现 |
| 发光 | illuminate | ❌ 无对战效果 |
| 捡拾 | pickup | ✅ BattleEngine.applyPickup 战斗结束时拾取已消耗道具 |
| 逃跑 | run-away | ❌ 未实现 |
| 采蜜 | honey-gather | ❌ 未实现 |
| 捡球 | ball-fetch | ✅ stub 注册 |

---

## 二、道具

### P1 - VGC 常用

| 道具 | 英文 ID | 状态 | 说明 |
|------|---------|------|------|
| 防尘护目镜 | safety-goggles | ✅ 粉末免疫 + 天气伤害免疫 |
| 突击背心 | assault-vest | ✅ 特防×1.5 + 封锁变化招式 |
| 弱点保险 | weakness-policy | ✅ 效果绝佳时物攻/特攻+2 并消耗 |
| 红牌 | red-card | ✅ 被攻击时强制换人，消耗 |
| 逃脱按键 | eject-button | ✅ 被攻击时换人（注意：未消耗，可多次触发，是已知 bug） |
| 逃脱背包 | eject-pack | ✅ 能力下降时触发实际换人 |
| 防晃护符 | covert-cloak | ✅ 免疫追加效果 |
| 吃剩的东西 | leftovers | ✅ 每回合回复 1/16 HP |
| 黑色污泥 | black-sludge | ✅ 毒系回血/非毒系扣血 |
| 火焰宝珠 | flame-orb | ✅ 回合结束时灼伤 |
| 剧毒宝珠 | toxic-orb | ✅ 回合结束时剧毒 |
| 力量 Herb | power-herb | ✅ 跳过蓄力回合 |
| 白色 Herb | white-herb | ✅ 恢复全部下降能力 |
| 洁净坠饰 | clear-amulet | ✅ 能力不被下降 |
| 焦点镜片 | scope-lens | ✅ 会心率+1 |
| 焦点带 | focus-band | ✅ 10% 濒死保留 1HP |
| 气势披带 | focus-sash | ✅ 满血被击倒时保留 1HP |
| 大葱 | leek | ✅ 大葱鸭会心率+2 |
| 幸运拳 | luck-punch | ✅ 吉利蛋会心率+2 |

### P2 - 树果类

| 道具 | 状态 | 说明 |
|------|------|------|
| 各种 HP 恢复果（橙橙果/文柚果等） | ✅ 已实现 |
| 混乱果（福禄果/Figy 等） | ✅ 已实现 |
| 减伤果（香罗果/利木果等） | ✅ getBerryResistFactor 全 18 种减伤果已实现 |
| 能力果（枝荔果/龙嘉果等） | ✅ HP<25% 对应能力+1 已实现 |
| 兰萨果 | ✅ 会心率 +2 阶级已实现 |
| 星桃果 | ✅ 随机能力 +2 已实现 |
| 奇迹果 | ✅ tryConsumeStatusBerry 已实现 |
| 解麻果/解毒果/解睡果等 | ✅ tryConsumeStatusBerry 全部已实现 |

### P3 - 进化携带/专属道具

| 道具 | 状态 | 说明 |
|------|------|------|
| 王者之证/锋利之爪 | ✅ 每段攻击独立畏缩 | 10% 畏缩已实现 |
| 金属膜/升级数据等 | ❌ 仅数据层，对战无效果 |
| 各系石板 | ✅ 阿尔宙斯属性变化 + 本系×1.2 |
| 各系卡带 | ✅ 银伴战兽属性变化 + 本系×1.2 |
| 各系存储碟 | ❌ 未实现（属性：空属性变化） |
| 朱红色花蜜/金黄色花蜜等 | ❌ 未实现（花舞鸟形态） |
| 沙奈朵进化石/艾路雷朵进化石等 | ✅ Mega 系统已实现 |

---

## 三、招式

### P1 - VGC 常见缺漏

| 招式 | 状态 | 说明 |
|------|------|------|
| 快速防守 | quick-guard | ✅ MoveRegistry + BattleRoundSupport |
| 广域防守 | wide-guard | ✅ MoveRegistry + BattleRoundSupport |
| 守住/看穿/挺住 | protect/detect/endure | ✅ MoveRegistry PROTECT_MOVES + BattleRoundSupport handleProtectionMove |
| 尖刺防守 | baneful-bunker | ✅ PROTECT_MOVES 已包含 |
| 长嚎 | howl | ✅ handleSupportMove 自身物攻+1 |
| 磨爪 | hone-claws | ✅ handleSupportMove 物攻+1 命中+1 |
| 龙之舞 | dragon-dance | ✅ 物攻+1 速度+1 |
| 蝶舞 | quiver-dance | ✅ 特攻+1 特防+1 速度+1 |
| 剑舞 | swords-dance | ✅ 物攻+2 |
| 诡计 | nasty-plot | ✅ 特攻+2 |
| 健美 | bulk-up | ✅ 物攻+1 物防+1 |
| 铁壁 | iron-defense | ✅ handleSupportMove 防御+2 |
| 冥想 | calm-mind | ✅ 特攻+1 特防+1 |
| 生长 | growth | ✅ handleSupportMove 物攻+1 特攻+1（晴天+2） |
| 腹鼓 | belly-drum | ✅ handleSupportMove 消耗 50%HP 物攻升至+6 |
| 破壳 | shell-smash | ✅ 攻/特攻/速+2 防/特防-1 |
| 戏法空间 | trick-room | ✅ 5 回合慢速先手 |
| 顺风 | tailwind | ✅ 4 回合速度翻倍 |
| 电气/精神/青草/薄雾场地 | electric/psychic/grassy/misty-terrain | ✅ 全部实现 |
| 求雨/大晴天/沙暴/雪景 | rain-dance/sunny-day/sandstorm/snowscape | ✅ 全部实现 |
| 黑雾 | haze | ✅ 重置全能力变化 |
| 清除之烟 | clear-smog | ✅ 清除目标能力变化 + 伤害 |
| 吼叫/吹飞 | roar/whirlwind | ✅ handleSupportMove 强制目标换人（Suction Cups/扎根/Substitute 可免疫） |
| 龙尾/巴投 | dragon-tail/circle-throw | ✅ processAction 伤害后强制目标换人 |

### P2 - 特殊机制招式

| 招式 | 状态 | 说明 |
|------|------|------|
| 替身 | substitute | ✅ 已实现 |
| 梦话/打鼾 | sleep-talk/snore | ✅ 睡眠中可用招式已实现 |
| 起死回生/抓狂 | reversal/flail | ✅ HP 越低威力越高已实现 |
| 拍落 | knock-off | ✅ 1.5 倍伤害 + 移除道具 |
| 杂技 | acrobatics | ✅ 无道具/道具已消耗时威力翻倍 |
| 小偷/渴望 | thief/covet | ✅ processAction damage 循环中伤害后偷取目标道具 |
| 迁怒/报恩 | frustration/return | ❌ 未实现 |
| 蓄力/喷出/吞下 | stockpile/spit-up/swallow | ❌ 未实现 |
| 打嗝 | belch | ❌ 未实现 |
| 自然之恩 | natural-gift | ❌ 未实现 |
| 虫灾 | bug-bite | ❌ 未实现 |

### P3 - 反伤/特殊代价招式

| 招式 | 状态 | 说明 |
|------|------|------|
| 勇鸟猛攻/闪焰冲锋/木槌/伏特攻击/舍身冲撞 | ✅ MoveRegistry RECOIL_MOVES 全部包含 |
| 地狱突刺 | throat-chop | ✅ 沉默声系招式 2 回合 |
| 深渊突刺 | jaw-lock | ✅ 束缚双方不可换下 |
| 背水一战 | no-retreat | ✅ 全能力+1 自身束缚 |
| 回复封锁 | heal-block | ✅ 已实现 |
| 定身法 | disable | ✅ 已实现 |
| 同命 | destiny-bond | ✅ 击倒时同归于尽 |
| 封印 | imprison | ✅ handleStatusMove 激活 + processAction isBlockedByImprison 封锁招式 |
| 临别礼物 | memento | ❌ 未实现 |

---

## 四、系统级功能

| 系统 | 状态 | 说明 | 优先级 |
|------|------|------|--------|
| Mega 进化系统 | ✅ 已实现 | Mega 石、形态变化 | P2 |
| Z 招式系统 | ✅ 已实现 | Z 晶体、Z 招式、专属 Z 招式 | P2 |
| 极巨化/太晶化 | ✅ 已实现 | 极巨招式、太晶化形态变化 | P2 |
| 双打队友目标选择 | ✅ 已实现 | BattleTargetSupport resolveMoveTargets | P2 |
| 回合结束阶段 | ✅ 已实现 | BattleTurnCleanupSupport 中毒/灼伤/寄生种子/束缚/灭亡之歌/水流环/扎根/恶梦 | P0 |
| 替身状态 | ✅ 已实现 | 替身实体、伤害吸收、挡状态 | P1 |
| 重力场 | ✅ 已实现 | BattleFieldEffectSupport | P1 |
| 戏法空间 | ✅ 已实现 | BattleFieldEffectSupport | P1 |
| 魔法空间 | ✅ | BattleFieldEffectSupport + handleSupportMove 激活 + isItemEffectActive 道具抑制 | P2 |
| 奇妙空间 | ✅ | BattleFieldEffectSupport + handleSupportMove 激活 + 伤害公式防御互换 | P2 |
| 变身为对方 | ❌ | 未实现 | P3 |
| 回收利用 | ❌ | 未实现 | P2 |

---

## 五、代码质量与测试

| 项目 | 说明 | 优先级 |
|------|------|--------|
| 特性效果单元测试 | 为 EffectRegistry 中 handler 编写独立测试 | P2 |
| 道具效果单元测试 | 为道具 handler 编写独立测试 | P2 |
| VGC 典型对局集成测试 | 模拟完整 VGC 对局场景 | P1 |
| 事件系统集成 | 将 event bus 接入引擎替代部分 EffectRegistry 调用 | P3 |

### 已知 Bug（本轮已修复）
- ~~**逃脱按键 (eject-button)**：触发换人后道具未消耗~~ ✅ 已修复——添加 consumeItem 调用
- ~~**紧张感 (unnerve)**：hasUnnerveOnOpposingSide 方法存在但树果消耗流程未调用~~ ✅ 已修复——树果块前检查 Unnerve
- ~~**逃脱背包 (eject-pack)**：能力下降时设置了标志但没有触发实际换人~~ ✅ 已修复——processAction 末尾扫描 ejectPackTriggered 并执行换人

---

## 六、审计说明

本文档于 2026-05-08 通过代码审计方法全面更新。审计范围包括：
- EffectRegistry.java — handler 注册
- BattleDamageSupport.java / BattleConditionSupport.java / BattleRoundSupport.java / BattleEngine.java — 硬编码逻辑
- BattleTurnCleanupSupport.java — 回合末处理
- MoveRegistry.java — 招式分类

> 注：大量特性在引擎中以 `engine.hasAbility(mon, "xxx")` 硬编码形式实现，
> 而非通过 EffectRegistry dispatch。两者任一实现即视为已实现。

## 统计

| 类别 | 已有 | 待补（VGC 重要） | 待补（全部） |
|------|------|------------------|-------------|
| 特性 | ~155+ 有逻辑 | ~0（P0 P1 全部完成） | ~8（P2/P3） |
| 道具 | ~97+ 有逻辑 | ~0 | ~5 |
| 招式 | ~510+ | ~6 | ~18 |
