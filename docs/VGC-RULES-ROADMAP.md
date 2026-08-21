# 对战引擎 VGC / Showdown 规则修复路线图

> 本文档汇总多轮深度审查（8+4+2 个并行代理）发现的对战引擎与 VGC / Showdown
> 规则差异，作为后续修复的执行蓝图。按严重度排序。
>
> 状态标记：#done=已完成并提交， #code=源码已改待编译验证， #todo=待修复

## 第一轮审查（8 类，21 项核心修复）— 已在 commit 02ce4e1 提交

见 `git log 02ce4e1`。涉及：属性相性表、spread 招式目标、回合结算、
换人规则、特性（大力士/破格/灾祸四宝等）、道具（抗性树果/颊囊等）、
伤害（灼伤+毅力/Tera STAB/Tinted Lens 等）、场地（天气不复活/雪天无伤）。

## 第二轮审查（4 类，28 项发现）

### A. VGC 格式与规则（8 项）

| Sev | 问题 | 位置 | 状态 |
|---|---|---|---|
| critical | 未知格式字符串静默落为双打，无格式校验 | BattleSetupSupport.java:40-47 | #todo |
| critical | 缺禁传说/受限宝可梦校验，任意队伍可开战 | BattlePreviewSupport.normalizePokemon / BattleSetupSupport | #todo |
| major | vgc63 被误当单打（应双打 2/4） | BattleSetupSupport.java:41,45 | #todo |
| major | 传说/幻兽排除过宽且只作用于随机池 | BattleDexMapper.java:33-34 | #todo |
| major | 缺格式级禁道具（心之水滴）/禁招式（暗黑洞） | ItemHandlers.java:93 / MoveRegistry | #todo |
| minor | 格式映射重复死代码 | BattleEngine.java:36-46 | #todo |
| minor | 12 回合上限硬编码多处重复 | BattleFlowSupport.java:33 / BattleService / BattleExecutor / BattleConfig | #todo |
| info | VGC 应禁用 Mega/Z/极巨化（仅太晶化） | BattleConfig.java:208-266 | #todo |

### B. 辅助/状态招式完整性（5 项）

| Sev | 问题 | 位置 | 状态 |
|---|---|---|---|
| critical | 天气型回复招式（光合/月光/晨光）传 null state 崩溃 | BattleConditionSupport.java:2445-2447 | #code（已加 state 参数，8 调用点已改） |
| major | 尖刺防守未注册为保护招式 | MoveRegistry.java:25-30 | #code（已加入 PROTECT_MOVES） |
| major | Eerie Impulse/Feather Dance/Scary Face/String Shot/Tickle/Noble Roar 等 no-op | BattleRoundSupport.java:2467 / MoveRegistry | #done（全部 8 种辅助降能招式已实现） |
| minor | 冷启回合（Chilly Reception）未实现 | MoveRegistry | #todo |
| minor | 治疗招式（治愈铃声等）队员时只看活体 | 待定位 | #todo |

### C. 目标系统与吸引机制（7 项）

| Sev | 问题 | 位置 | 状态 |
|---|---|---|---|
| major | 狙击射击（tracksTarget）误绕避雷针/引水 | BattleTargetSupport.java:136-140 | #code（已拆分） |
| major | 避雷针/引水优先级低于看我嘛（应特性级优先） | BattleTargetSupport.java:141-144,164-172 | #code（已调整单目标+随机目标） |
| major | applySleep 把所有睡眠招式按粉末免疫 | BattleConditionSupport.java:210-213 | #code（已限定仅孢子/睡眠粉） |
| minor | 能力吸引未限定伤害招式（Soak 不该被引水吸引） | BattleTargetSupport.java:252-281 | #code（已加 power<=0 判定） |
| minor | target_id 13 注释与实现冲突，被计 0.75 spread | BattleDamageSupport.java:608 | #todo |
| info | spread 0.75 衰减核对通过 | BattleRoundSupport:299-313 | 已确认正确 |
| info | 坚毅/尾翼无视吸引、破格仅无视引水/避雷针 | BattleTargetSupport:117,136-139 | 已确认正确 |

### D. 随机组队系统质量（8 项）

| Sev | 问题 | 位置 | 状态 |
|---|---|---|---|
| major | isPhysical 判定恒 true，特攻手被错发 Choice Band | AIService.java:460-472 | #code（已改为 atk>=spa 判定） |
| major | 只生成攻向构建，无坦克/辅助定位 | AIService.java:290-389 | #done（determineBuild 新增 tank/support + 防御性格/EV/道具） |
| major | BattleScore 强度评分饱和失真，无法分层 | TeamBalanceEvaluator.java:36,367 | #todo |
| major | validatePlayability 误拒生命宝珠+火/电 | AIService.java:278-282 | #todo |
| minor | 慢速宝可梦可能同时拿突击背心与变化招 | AIService.java:474-475,637-665 | #todo |
| minor | seed 无法复现团队（SQL RANDOM + ThreadLocalRandom） | AIService.java:60-61,478 | #todo |
| minor | AI 从不使用强化招式（剑舞/龙舞等） | BattleDecisionSupport.java:54-116 | #todo |
| minor | Z 招式覆盖已选道具且 heldItemInfo 陈旧 | AIService.java:216,859-862 | #todo |

## 第三轮审查（特性 12 项 + 道具 5 项）

### E. 特性机制（12 项）

| Sev | 问题 | 位置 | 状态 |
|---|---|---|---|
| critical | 干燥肌肤缺天气交互（雨回 1/8、晴损 1/8） | BattleTurnCleanupSupport 回合末天气 | #code（已加 dry-skin 雨回/晴损） |
| major | 怪力钳 Hyper Cutter 未注册（挡攻击下降） | EffectRegistry / isStatDropBlocked | #code（已加 hardcode + 威吓名单） |
| major | 心灵感应 Telepathy 未注册（免疫队友群伤） | BattleRoundSupport spread 循环 | #code（已加 telepathy 跳过） |
| major | 雪之力 Snow Force 未注册（雪天冰系 1.3x） | EffectRegistry | #code（已注册） |
| info | 太阳之力缺晴天 1/8 损耗 | 回合末天气 | #code（已加 solar-power 晴损） |
| minor | 压迫感 Pressure 未接 PP 系统 | EffectRegistry.java:1038-1041 | #done（BattleEngine:439 已有实现：对方 Pressure 时多扣 1 PP） |
| minor | 引爆 Aftermath 触发时机（应被击倒时才触发） | EffectRegistry.java:890-896 | #todo |
| 说明 | Guts×灼伤：第一轮 0.75x 修复正确（Showdown 中灼伤×0.5 与 Guts×1.5 独立相乘），特性子代理的 1.5x 判断有误，不回退 | − | 已确认 |
| 说明 | 魔法防守/威吓/先制控速/加速/踩影等已正确 | 多处 | 已确认 |
| info | 久经沙场/拟态兽：disguise 已实现挡一击 | BattleRoundSupport.java:664 | 已确认 |

### F. 道具机制（5 项）

| Sev | 问题 | 位置 | 状态 |
|---|---|---|---|
| major | 节拍器数值错误（0.2 应为 0.1）+ 封顶 6→10 | ItemHandlers.java:38-41 / BattleEngine.java:1491 | #code（已修 0.1 + 封顶 2.0x + 计数 10） |
| major | 红牌未要求接触伤害 | BattleEngine.java:1222-1227 | #code（已加 isContactMove） |
| major | 驱动能量 drive-energy 缺失 | EffectRegistry.java:2752 | 误报——实际名 booster-energy 已实现，无需修 |
| minor | 节拍器计数默认值不一致 | BattleEngine.java:1492 | #code（已统一） |
| minor | 场地震子未校验接地 | BattleConditionSupport.java:1789-1817 | #code（已加 isGrounded 校验） |

### G. 前端 UI 对标（12 项，第 4 轮审查）

| Sev | 问题 | 位置 | 状态 |
|---|---|---|---|
| critical | 完全缺失能力阶级(+X/-X)显示 | BattleArena.vue conditionBadges:655-699 / BattleDecisionPanel:293-316 | #done（+N Atk/-N SpD 蓝/红色徽章） |
| major | 天气/场地/双墙/钉子无剩余回合数 | BattleArena.vue fieldEffectChips:709-741 | #done（显示 Nt 后缀如 "雨天 3T"） |
| major | 无法点击在场精灵查看完整配置 | BattleArena.vue:130-146 | #done（ℹ按钮+右键打开 PokemonDetailPopover） |
| major | 精灵无攻击/受击动画 | BattleArena.vue:770-808 | #todo |
| major | 键盘快捷键 L 标记首发起死代码 | Battle.vue:490-494 | #done（L键循环切换首发标记） |
| minor | 移动端战场横向溢出 | BattleArena.vue:817 | #todo |
| minor | 回合日志不可折叠、无限加长 | BattleArena.vue:363-431 | #todo |
| minor | 队伍预览放大镜按钮视觉噪音 | BattleDecisionPanel.vue:62-87 | #todo |
| info | 决策面板在场精灵头部信息过少 | BattleDecisionPanel.vue:293-316 | #todo |
| info | 招式按钮无按克制着色 | MoveButton.vue | #done（绿/红/蓝边框按克制倍率着色） |
| info | 天气视觉仅雨有粒子动画 | BattleArena.vue:942-956 | #todo |
| info | 无回合计时/时钟 | BattleArena.vue | #todo |

### H. 随机对战 AI 质量（15 项，第 4 轮审查）

| Sev | 问题 | 位置 | 状态 |
|---|---|---|---|
| critical | AI 难度体系（Easy/Normal/Hard/Expert）是死代码，真实路径无难度参数，四档零差异 | AIStrategy.java / AIDifficulty.java / BattleActionBuilder.java:49 | #done（难度已接入 selectAIMove，strategicChance 四档缩放） |
| critical | AI 从不主动使用强化技（剑舞/冥想/龙舞等） | BattleDecisionSupport.java:54-142 | #done（新增 selectAISetupMove 16 种强化招式+安全判定） |
| major | 目标选择与招式打分脱节，双打随机挑目标不查免疫 | BattleActionBuilder.java:68-77 | #done（selectBestTargetSlot 类型克制+低血补刀） |
| major | 换人一维属性最小化 + 随机概率门控 | BattleAiSwitchSupport.java:43-87 | #done（多维评分：typeResist+HP+offense+OHKO惩罚） |
| major | selectBestDamageMove 忽略命中率/次要效果/必杀线 | BattleDecisionSupport.java:145-185 | #done（+accuracy权重+ailment/flinch加分+KO bonus） |
| major | 难度宣称能力未实现（伤害预测占位、克制固定 1.0） | AIStrategy.java:52-56 | #todo |
| minor | AI 不用 Protect/撒钉/场地战略 | BattleDecisionSupport.java:194 | #todo |
| minor | 资源招只随机放行无收益权衡 | BattleAISupport.java:47-198 | #todo |
| info | 换人评估用 base types 而非 activeTypes | BattleAiSwitchSupport.java:212 | #done（已改用 engine.activeTypes） |
| info | heavilyDebuffed 判断粗糙 | BattleAiSwitchSupport.java:40-60 | #done（加权攻击/速度下降×2） |
| info | 双打换人逐只独立决策可能连环对位 | BattleActionBuilder.java:54-64 | #todo |
| info | findBestDefensiveSwitch 无被一击死兜底 | BattleAiSwitchSupport.java:224-243 | #done（OHKO 惩罚 -50 分） |
| info | 无终局资源意识 | 全局 | #todo |
| info | evaluateThreatLevel 是死代码从未调用 | BattleAnalysisSupport.java:218-251 | #todo |
| info | 睡眠/哈欠仅 40% 随机放行 | BattleAISupport.java:29-46 | #todo |

## 执行优先级建议

1. **编译验证已改代码**（#code 标记多处）：恢复环境后 `mvn compile -pl battle -am`，
   通过后提交。
2. A 类格式校验与 vgc63 双打化（critical/major）。
3. B 类辅助招式补全（Feather Dance/Eerie Impulse/Scary Face 等用现有 applyXxxDrop）。
4. E 类剩余（Pressure PP、Aftermath 时机）+ F 类场地震子接地。
5. D 类随机组队系统（坦克构建、强度评分、生命宝珠误拒）。
6. 回归验证 + UI 完善 + 代码解耦。

## 环境恢复后的验证命令

```powershell
# 编译验证
cd backend && mvn compile -pl battle -am -DskipTests -q
# 打包重启
Remove-Item battle\target\battle-0.0.1-SNAPSHOT.jar -Force
mvn package -pl battle -am -DskipTests "-Dmaven.test.skip=true" -q
java -jar battle\target\battle-0.0.1-SNAPSHOT.jar
# git 提交
git add -A && git commit -m "feat(battle): ..."
```
