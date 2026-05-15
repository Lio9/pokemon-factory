package com.lio9.battle.effect;

import java.util.List;
import java.util.Map;

/**
 * 特性处理器接口。
 * <p>
 * 每个特性一个实现类，通过 EffectRegistry 注册。
 * 引擎在对应 hook 点遍历所有相关特性并调用 handler。
 * 仿照 Pokemon Showdown 的 abilities.ts 设计。
 * </p>
 *
 * <pre>
 * // 新增特性只需：
 * // 1. 实现 AbilityHandler
 * // 2. 在 EffectRegistry 注册一行
 * // 3. 引擎代码不用动
 * </pre>
 */
public interface AbilityHandler {

    /** 特性唯一标识（name_en，小写） */
    String id();

    // ====== 伤害修正钩子 ======

    /** 攻击方特性：修正最终伤害倍率（before random factor） */
    default double onSourceModifyDamage(AttackContext ctx, double modifier) { return modifier; }

    /** 防御方特性：修正最终伤害倍率（before random factor） */
    default double onTargetModifyDamage(AttackContext ctx, double modifier) { return modifier; }

    /** 攻击方特性：修正攻击属性值（物理时 attack，特殊时 specialAttack） */
    default int onSourceModifyAttackStat(AttackContext ctx, int stat) { return stat; }

    /** 防御方特性：修正防御属性值（物理时 defense，特殊时 specialDefense） */
    default int onTargetModifyDefenseStat(AttackContext ctx, int stat) { return stat; }

    /** 攻击方特性：修正招式威力 */
    default int onSourceModifyMovePower(AttackContext ctx, int power) { return power; }

    /** 修正招式属性类型（用于 -ate 特性） */
    default int onSourceModifyMoveType(AttackContext ctx, int moveTypeId) { return moveTypeId; }

    // ====== 速度修正 ======

    /** 修正速度值 */
    default int onModifySpeed(SpeedContext ctx, int speed) { return speed; }

    // ====== 能力阶级变化 ======

    /** 能力阶级变化前回调。返回反转后的 delta，0 表示不变 */
    default int onModifyStatStage(StatStageContext ctx, int delta) { return delta; }

    // ====== 免疫/阻挡 ======

    /** 属性免疫检查。返回 true 表示免疫 */
    default boolean onTypeImmunity(ImmunityContext ctx) { return false; }

    /** 状态免疫检查。返回 true 表示免疫 */
    default boolean onStatusImmunity(StatusContext ctx) { return false; }

    /** 精神类控制免疫（taunt/attract/disable/encore/torment）。返回 true 表示免疫 */
    default boolean onMentalImmunity(StatusContext ctx) { return false; }

    /** 修正追加效果发动概率（如天恩翻倍） */
    default int onModifySecondaryEffectChance(int chance) { return chance; }

    /** 是否阻挡追加效果（如鳞粉） */
    default boolean onBlocksSecondaryEffects() { return false; }

    /** 是否阻挡自身能力被对手下降（恒净之躯/白烟/金属防护） */
    default boolean onBlocksStatDrop() { return false; }

    // ====== 回合流程钩子 ======

    /** 上场时触发（包含开场、换入、复活） */
    default void onSwitchIn(SwitchInContext ctx) {}

    /** 使用招式前 */
    default void onBeforeMove(BeforeMoveContext ctx) {}

    // ====== 接触触发 ======

    /** 被接触招式命中时触发（静电、火焰之躯等） */
    default void onContact(ContactContext ctx) {}

    // ====== 重量修正 ======

    /** 修正重量（用于重磅冲撞/高温重压） */
    default int onModifyWeight(WeightContext ctx, int weight) { return weight; }

    /** 受到伤害后触发（伤害计算并应用后） */
    default void onDamageReceived(DamageReceivedContext ctx) {}
}
