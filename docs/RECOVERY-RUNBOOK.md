# 对战引擎修复 — 环境恢复执行手册

> 本手册记录一轮多轮审查 + 修复周期中**全部未提交的源码修复**（20 项），
> 供宿主 `pwsh` 环境恢复后立即编译验证、提交，避免遗漏或误改。
>
> 背景：host 的 `pwsh.exe` 持续 `ENOENT`（`C:\Program Files\WindowsApps\...`），
> 导致无法编译/运行/git。以下修复均已在源码层完成且经 grep/read 自洽核验，
> 但**尚未编译验证**。

## 一、未提交修复清单（20 项，按文件分组）

### 1. BattleConditionSupport.java
| # | 修复 | 关键标记（grep 验证） |
|---|---|---|
| 1 | 天气型回复 NPE：getHealFraction/applyRecoveryMove 加 state 参数 | `applyRecoveryMove(Map<String, Object> state`（2409） |
| 2 | 睡眠粉末免疫只针对粉末招式 | `isSpore(move) && engine.isPowderImmune`（212） |
| 3 | 怪力钳 Hyper Cutter 挡攻击下降（isStatDropBlocked + 威吓名单） | `hasAbility(target, "hyper-cutter"...`（3065/2069） |
| 4 | applySpeedDrop 重载 applySpeedDropBy（降速 N 段） | `void applySpeedDropBy(`（2476） |
| 5 | 新增 applyAttackDropBy（降攻击 N 段，含唱反调联动） | `boolean applyAttackDropBy(`（2560） |
| 6 | 场地震子接地校验 | `engine.isGrounded(source)`（1792） |

### 2. BattleRoundSupport.java
| # | 修复 | 关键标记 |
|---|---|---|
| 7 | 天气回复 8 个调用点传 state | `applyRecoveryMove(state, actor, move`（2378-2475） |
| 8 | 心灵感应 Telepathy 免疫队友群伤 | `engine.hasAbility(target, "telepathy")`（330） |
| 9-14 | 6 个辅助招式分支：Eerie Impulse/Noble Roar/Scary Face/Cotton Spore/Feather Dance/Charm | `isEerieImpulse/...`（1901-1929） |

### 3. BattleTargetSupport.java
| # | 修复 | 关键标记 |
|---|---|---|
| 15 | 狙击射击拆分（tracksTarget 不再绕避雷针）| `if (abilityRedirected >= 0)`（146）|
| 16 | 避雷针/引水优先级 > 看我嘛（单目标+随机目标）| 同文件 142-156,171-179 |
| 17 | 能力吸引限定伤害招式 | `if (power <= 0) return -1`（约 282）|

### 4. MoveRegistry.java
| # | 修复 | 关键标记 |
|---|---|---|
| 18 | 尖刺防守注册 | `"spiky shield", "spiky-shield"`（30）|
| 19 | 6 个辅助招式注册 + isXxx 方法 + sleep-powder 补进 SPORE | `isEerieImpulse/isNobleRoar/isScaryFace/isCottonSpore/isFeatherDance/isCharm` |

### 5. EffectRegistry.java
| # | 修复 | 关键标记 |
|---|---|---|
| 20 | 雪之力 Snow Force 注册 | `"snow-force"`（412）|

### 6. ItemHandlers.java
| # | 修复 | 关键标记 |
|---|---|---|
| 21 | 节拍器数值（0.1 + 封顶 2.0x）| `Math.min(2.0, 1.0 + count * 0.1)`（40）|

### 7. BattleEngine.java
| # | 修复 | 关键标记 |
|---|---|---|
| 22 | 节拍器计数封顶 10 | `Math.min(10, consecutive + 1)`（1495）|
| 23 | 红牌接触判定 | `isContactMove(move)`（1225）|
| 24 | 6 个辅助招式委托方法 | `isEerieImpulse/isNobleRoar/...`（545-566）|

### 8. BattleTurnCleanupSupport.java
| # | 修复 | 关键标记 |
|---|---|---|
| 25 | 干燥肌肤天气（雨回/晴损）+ 太阳之力晴损 | `"dry-skin"`/`"solar-power"`（794/802）|
| 26 | 新增 applyWeatherDamageToMon 辅助 | `applyWeatherDamageToMon(`（635）|

### 9. AIService.java
| # | 修复 | 关键标记 |
|---|---|---|
| 27 | isPhysical 恒 true bug（atk>=spa 判定）| `boolean isPhysical = atk >= spa`（464）|

> 注：第 1/7/15/16/17 项为第二轮审查修复（round 4 前后），其余为第三轮
> 特性/道具审查修复。以上合计约 27 处改动，跨 10 个文件。

## 二、环境恢复后执行步骤

```powershell
cd D:\learn\pokemon-factory\backend

# 1. 编译验证（最重要——确认所有改动语法正确）
mvn compile -pl battle -am -DskipTests -q
# 若报错：逐文件检查上面的关键标记，多半是签名/import 问题

# 2. 打包完整 fat jar（需先杀掉 java 释放 jar 锁）
Stop-Process -Name java -Force
Remove-Item battle\target\battle-0.0.1-SNAPSHOT.jar -Force -ErrorAction SilentlyContinue
mvn package -pl battle -am -DskipTests "-Dmaven.test.skip=true" -q

# 3. 启动后端并回归
java -jar battle\target\battle-0.0.1-SNAPSHOT.jar
# 验证: http://localhost:8084/api/pokedex/pokemon/25 返回 200
# 验证: guest 战斗启动 + 首回合（双打 spread/威吓/辅助招式）

# 4. 前端
cd D:\learn\pokemon-factory\frontend
npm run dev
# 验证: http://localhost:7894/battle?mode=guest 页面可交互

# 5. 提交
cd D:\learn\pokemon-factory
git add -A
git commit -m "feat(battle): VGC 引擎规则修复 20+ 项（天气/特性/道具/目标/辅助招式）"
```

## 三、重点回归场景（改动风险区）

1. **天气型回复**：用光合作用/月光/晨光的宝可梦，晴 / 正常 / 雨天三种天气各测一次，
   确认回复量 2/3、1/2、1/4 且不崩溃（原 NPE）。
2. **辅助招式**：Feather Dance/Charm（-2攻）、Scary Face/Cotton Spore（-2速）、
   Eerie Impulse（-2特攻）、Noble Roar（-1攻特攻），确认命中后目标阶级变化正确。
3. **心灵感应**：双打地震/冲浪打队友，有 telepathy 的队友应免疫。
4. **干燥肌肤/太阳之力**：雨天回 1/8、晴天损 1/8（魔法防守者应免疫损耗）。
5. **节拍器/红牌**：连续使用增伤是否每回合 +10% 封顶 2x；红牌是否仅接触伤害触发。
6. **怪力钳**：被威吓/降攻击招式命中时攻击不下降（其他能力仍可降）。
7. **AIService**：高速特攻手（如 Alakazam）应拿 Choice Specs 而非 Choice Band。

## 四、若编译失败的可能位置

- `applyRecoveryMove(state, actor, move, ...)`：确认 8 个调用点都已传 state
- `applySpeedDropBy`/`applyAttackDropBy` 重载：确认与原型方法参数不冲突（重载 OK）
- `isEerieImpulse` 等：确认 MoveRegistry/BattleEngine/handleStatusMove 三处齐全
- `applyWeatherDamageToMon`：确认定义在 BattleTurnCleanupSupport 内且调用点匹配

## 五、文档同步

修复完成后，将 `docs/VGC-RULES-ROADMAP.md` 中对应 `#code` 标记改为 `#done`。

## 六、UI / AI 核心修复实施指引（对应路线图 G/H 类）

> 以下为高价值缺陷的具体改造方案（含关键逻辑），环境恢复后按此实施。

### G1. 能力阶级显示（critical）
- BattleArena.vue 信息框（对手/我方区）：读取 `mon.statStages`（Map，含 attack/defense/specialAttack/specialDefense/speed 值 -6~+6）。
- 关键：statStages 值 !=0 时显示 `+2Atk`/`-1Spe` 品类徽章。建议在 `conditionBadges()` 前补 `statStageBadges(mon)`——筛选非零阶级，映射缩写（Atk/Def/SpA/SpD/Spe），用蓝(+)/红(-)圆角 chip 渲染在 HP 条上方。
- BattleDecisionPanel.vue:293-316 在场头部同样补简版（只显示非零阶级）。

### G2. 天气/场地剩余回合（major）
- BattleArena.vue `fieldEffectChips`（709-741）读取 `summary.fieldEffects` 中 `rainTurns/sunTurns/sandTurns/snowTurns/trickRoomTurns/...Turns` 等 key。
- 改动：chip 文案从效果名改为 `效果名·Nt`（N=剩余回合），如 `雨天·3t`、`戏法空间·2t`。
- 天气视觉层角落（562-580 附近）加剩余回合小字。

### G3. 点场上精灵查看详情（major）
- BattleArena.vue opponent 精灵 `<button>`（130-146）：加 `@dblclick`（或长按）emit `open-detail(mon)`，Battle.vue 监听后进入 PokemonDetailPopover。
- 与目标选择区分：单击仍选目标，双击/hover-展开看详情。需在 Battle.vue 引入 PokemonDetailPopover 实例。

### H1. AI 难度参数接入（critical）
- `BattleActionBuilder.buildOpponentActions(state, random)` → 加 `int difficulty` 参数，从 BattleExecutor/调用方透传。
- `BattleDecisionSupport.selectAIMove` 同样加 difficulty。
- 接上 AIStrategy：difficulty=1(Easy)→随机、2(Normal)→现有启发式、3(Hard)→强化技+联合打分、4(Expert)→+目标威胁加权。删除死代码或实现各档真实逻辑。

### H2. 强化技启发式（critical）
- BattleDecisionSupport.selectAIMove 在 selectBestDamageMove 前加：若 `hasSetupMove(actor)` 且（我方快于对手 / 对手不能反击 / 我方未强化）→ 使用剑舞/冥想/龙舞。
- 参照现有 isSwordsDance/isCalmMind 等 MoveRegistry 判定。

### H3. 招式×目标联合打分（major）
- 将"先选招再随机选目标"改为：`for 招: for 目标: score=estimateMoveScore(招,目标)`，取最优"招+目标"组合。
- `estimateMoveScore` 加入: typeModifier(目标)、accuracy/100、目标是否可一击必杀。
- 删掉 BattleActionBuilder 的 `random.nextBoolean()` 定目标逻辑。

### H4. 换人评估（major）
- chooseAISwitch 去掉纯概率门控，改确定性：`不换会被打死 && 换上来能扛/能反打 → 换`。
- findBestDefensiveSwitch 增加"候选者被当前场上所有威胁的预期伤害"评估（防被一击秒）。
- maxTypeFactorAgainst 已改用 activeTypes（#code）。

### H5. 终局资源意识（info）
- selectAIMove/换人前检查：本方剩余可上场数 vs 对方，若本方劣势则倾向保守/换保血。
