# Common 模块职责边界清单

最后更新：2026-04-13
范围：只梳理 common 与 pokeDex 的职责边界，不调整数据库引擎、不重写初始化链路

## 1. 目标

当前 common 模块不只是“共享基础设施”，还混入了大量 pokeDex 业务代码。这个问题会直接带来三类成本：

- common 的改动容易触发 pokeDex 行为变化
- 模块边界不清楚，battleFactory 和其他模块更难判断该依赖谁
- 后续做 Android 迁移准备时，接口和业务契约难以稳定归位

这份清单的目标不是立刻重写模块，而是先把“谁应该留在 common，谁应该迁走”说清楚。

## 2. common 应该保留什么

以下内容应继续保留在 common，因为它们属于共享基础设施：

### 2.1 数据库与初始化基础设施

- `config/CommonDatabasePathResolver`
- `config/CommonDataSourceConfig`
- `config/CommonDatabaseProperties`
- `config/CommonDatabaseInitializer`
- `config/CommonCsvDataImporter`

这些类负责：

- 统一 SQLite 路径解析
- 统一 DataSource / Hikari 配置
- 数据库初始化与 CSV 导入链路

这部分是当前嵌入式数据库方案的核心地基，不应因为边界整理被挪走。

### 2.2 通用响应模型

- `response/ResultResponse`
- `response/ResponseCode`

这部分已经被多个模块复用，继续留在 common 是合理的。

### 2.3 共享启动入口

- `CommonApplication`

它目前承担数据库和共享初始化入口的角色，可以继续保留。

## 3. common 不应该继续承载什么

以下内容从职责上看更适合放进 pokeDex，而不是 common。

### 3.1 业务实体 model

这部分已完成迁移。

当前状态：

- 图鉴业务 model 已全部迁入 `com.lio9.pokedex.model`
- common 已不再保留 `Pokemon`、`PokemonForm`、`PokemonMove`、`Ability`、`Move`、`Type`、`Item`、`EvolutionChain`、`EggGroup`、`GrowthRate` 等图鉴域实体
- pokeDex 侧 model 包替换后，后端 reactor `mvn clean test` 已完整通过

这些对象已经回到图鉴域自身，不再作为“共享基础设施”挂在 common 下。

### 3.2 图鉴业务 mapper

这部分已完成迁移。

当前状态：

- 图鉴业务 mapper 已全部迁入 `com.lio9.pokedex.mapper`
- `PokemonMapper.xml` 已随 `PokemonMapper` 一起迁入 pokeDex 资源目录
- `PokeDexApplication` 已只扫描 `com.lio9.pokedex.mapper`

common 当前不再承载图鉴业务 mapper。

### 3.3 图鉴业务 service 与实现

当前 common 下的 `service/` 与 `service/impl/` 也已经是图鉴业务层，例如：

- `PokedexService`
- `PokemonService`
- `MoveService`
- `AbilityService`
- `TypeService`
- `ItemService`
- `EvolutionChainService`
- `DamageCalculatorService`

其中尤其需要明确：

- `PokedexService` 明确属于 pokeDex 业务编排
- `DamageCalculatorService` 虽然未来可能被别的模块复用，但本质仍是业务解算服务，不是基础设施

### 3.4 业务 VO / DTO

当前 common 下的 `vo/` 也基本围绕图鉴查询和伤害计算，例如：

- `PokemonDetailVO`
- `PokemonListVO`
- `MoveVO`
- `AbilityVO`
- `ItemVO`
- `TypeVO`
- `EvolutionChainVO`

这些对象应该跟着 pokeDex API 和服务层一起迁走。

### 3.5 业务算法 util

- `util/DamageCalculatorUtil`

这类 util 虽然名字叫 util，但职责上仍然是图鉴和对战业务算法的组成部分，不应继续挂在 common 下伪装成基础设施。

## 4. 推荐的目标边界

整理后的职责建议如下：

### common 最终负责

- 数据源配置
- 数据库路径解析
- 初始化和 CSV 导入
- 通用响应对象
- 如后续需要，再容纳少量真正跨模块的异常模型

### pokeDex 最终负责

- 图鉴域 model
- 图鉴域 mapper
- 图鉴查询 service
- 伤害计算 service
- 图鉴查询返回的 VO / DTO
- 与图鉴相关的 util / adapter

## 5. 推荐迁移顺序

### 第一步：先出包结构，不动数据库链路

在 pokeDex 中补齐以下包：

- `com.lio9.pokedex.model`
- `com.lio9.pokedex.mapper`
- `com.lio9.pokedex.service`
- `com.lio9.pokedex.service.impl`
- `com.lio9.pokedex.vo`
- `com.lio9.pokedex.util`

### 第二步：先迁业务 VO / service / mapper

当前进度：图鉴 service / service.impl、vo、全部 mapper，以及 `DamageCalculatorUtil` 已完成迁入 pokeDex。

优先迁：

- `PokedexService`
- `DamageCalculatorService`
- 与它们直接相关的 mapper 和 VO

原因：

- 这是边界最清楚的一层
- controller 到 service 的调用链最容易验证

### 第三步：再迁 model

当前进度：已完成。

结果：

- 图鉴实体已从 common 迁入 pokeDex
- `PokemonMapper.xml` 的 `resultType` 已切到 `com.lio9.pokedex.model.*`
- 迁移过程中只调整 Java 包归属，不改数据库表结构，也不改 CSV 初始化逻辑

### 第四步：最后收紧 common 的扫描范围

当前进度：核心收口已完成。

- `PokeDexApplication` 已只扫描 `com.lio9.pokedex.mapper`
- battleFactory 已显式依赖 pokeDex 复用图鉴相关 mapper / util
- 后续关注点已从 common / pokeDex 边界收缩，转回 BattleEngine 深拆和更细的集成测试补齐

## 6. 迁移时必须守住的约束

- 不改数据库引擎
- 不改 SQLite 初始化链路
- 不改 CSV 数据来源
- 不重写 `CommonDatabaseInitializer`
- 不把 common 重新做成新的业务模块

这轮只处理 Java 代码归属和模块职责，不碰数据库层方案。

## 7. 验收标准

等真正开始迁移时，至少要满足以下验收点：

- pokeDex 能独立编译并启动
- 现有图鉴接口返回不变
- 伤害计算接口结果不回归
- common 中不再残留图鉴业务 service / mapper / model / vo

当前状态补充：service、vo、mapper、model 与图鉴计算 util 已从 common 迁出，后端 reactor `mvn clean test` 已通过。
- battleFactory 如果需要图鉴或伤害计算能力，显式依赖 pokeDex，而不是继续隐式吃 common 里的业务代码

## 8. 当前结论

当前 common 的真实问题不是“代码太多”，而是“基础设施层和图鉴业务层混在一起”。

这一轮边界收缩已经证明：

1. 可以在不动数据库方案的前提下，把图鉴业务完整收回 pokeDex。
2. common 可以重新聚焦为共享基础设施层，而不是继续承载图鉴域实现。
3. 下一阶段更合理的动作，是继续拆 BattleEngine 内部职责并补更细粒度的 integration / contract test。
