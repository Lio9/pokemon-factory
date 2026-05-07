package com.lio9.battle.engine.event;

import com.lio9.battle.effect.BeforeMoveContext;
import com.lio9.battle.effect.EffectRegistry;
import com.lio9.battle.effect.StatusContext;
import com.lio9.battle.effect.SwitchInContext;

import java.util.Map;

/**
 * 将 EffectRegistry 中已注册的 handler 包装为事件总线监听器。
 * <p>
 * 纯函数式 handler（伤害倍率、属性修正等）保留在 EffectRegistry 中通过静态 dispatch 调用；
 * 触发式 handler（免疫、上场触发等）通过此桥接到事件总线。
 * </p>
 * 注意：EffectRegistry 中的 dispatchMoveBlock / dispatchContact / dispatchDamageReceived 等方法
 * 已在 BattleConditionSupport 中通过直接调用处理，此处不重复桥接。
 */
public final class EventRegistryBridge {

    private EventRegistryBridge() {}

    /**
     * 将所有 EffectRegistry handler 注册到事件总线上。
     */
    public static void registerHandlers(BattleEventBus bus) {
        // 状态免疫 → ON_SET_STATUS
        bus.registerHandler(BattleEventType.ON_SET_STATUS, new StatusImmunityBridge());

        // 上场触发 → ON_SWITCH_IN
        bus.registerHandler(BattleEventType.ON_SWITCH_IN, new SwitchInBridge());

        // 出招前 → ON_BEFORE_MOVE
        bus.registerHandler(BattleEventType.ON_BEFORE_MOVE, new BeforeMoveBridge());
    }

    // ========================================================================
    //  内部桥接 handler
    // ========================================================================

    /**
     * 状态免疫 → EffectRegistry.dispatchStatusImmunity
     */
    private static class StatusImmunityBridge implements BattleEventHandler {
        @Override
        public EventResult handle(BattleEvent event, Map<String, Object> context) {
            Map<String, Object> target = event.getTarget(context);
            if (target == null) return EventResult.CONTINUE;

            String condition = (String) context.get("condition");
            if (condition == null || condition.isEmpty()) return EventResult.CONTINUE;

            boolean immune = EffectRegistry.dispatchStatusImmunity(target, new StatusContext(target, condition, null));
            return immune ? EventResult.IMMUNE : EventResult.CONTINUE;
        }

        @Override
        public int getPriority() { return 50; }

        @Override
        public String getSource() { return "bridge:status-immunity"; }
    }

    /**
     * 上场触发 → EffectRegistry.dispatchSwitchIn
     */
    private static class SwitchInBridge implements BattleEventHandler {
        @Override
        public EventResult handle(BattleEvent event, Map<String, Object> context) {
            Map<String, Object> mon = event.getSource(context);
            if (mon == null) return EventResult.CONTINUE;

            Map<String, Object> state = (Map<String, Object>) context.get("state");
            boolean playerSide = Boolean.TRUE.equals(context.get("playerSide"));

            EffectRegistry.dispatchSwitchIn(new SwitchInContext(mon, state, playerSide));
            return EventResult.CONTINUE;
        }

        @Override
        public int getPriority() { return 0; }

        @Override
        public String getSource() { return "bridge:switch-in"; }
    }

    /**
     * 出招前 → EffectRegistry.dispatchBeforeMove
     */
    private static class BeforeMoveBridge implements BattleEventHandler {
        @Override
        public EventResult handle(BattleEvent event, Map<String, Object> context) {
            Map<String, Object> actor = event.getSource(context);
            Map<String, Object> move = event.getMove(context);
            Map<String, Object> state = (Map<String, Object>) context.get("state");
            if (actor == null || move == null) return EventResult.CONTINUE;

            boolean playerSide = Boolean.TRUE.equals(context.get("playerSide"));
            EffectRegistry.dispatchBeforeMove(new BeforeMoveContext(actor, move, state, playerSide));
            return EventResult.CONTINUE;
        }

        @Override
        public int getPriority() { return 0; }

        @Override
        public String getSource() { return "bridge:before-move"; }
    }
}
