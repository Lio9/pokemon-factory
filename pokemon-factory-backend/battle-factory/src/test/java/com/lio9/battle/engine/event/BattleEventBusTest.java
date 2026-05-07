package com.lio9.battle.engine.event;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BattleEventBusTest {

    @Test
    void registerAndFire_returnsContinueWhenNoHandlers() {
        BattleEventBus bus = new BattleEventBus();
        bus.freeze();
        EventResult result = bus.fireEvent(BattleEventType.ON_TURN_START,
            new BattleEvent(BattleEventType.ON_TURN_START) {},
            Map.of());
        assertTrue(result.shouldContinue());
    }

    @Test
    void registerAndFire_handlerCanStopEvent() {
        BattleEventBus bus = new BattleEventBus();
        bus.registerHandler(BattleEventType.ON_BEFORE_MOVE, new BattleEventHandler() {
            @Override public EventResult handle(BattleEvent event, Map<String, Object> context) {
                return EventResult.STOP;
            }
            @Override public String getSource() { return "test:stopper"; }
        });
        bus.freeze();

        EventResult result = bus.fireEvent(BattleEventType.ON_BEFORE_MOVE,
            new ModifyPowerEvent(100), Map.of());
        assertFalse(result.shouldContinue());
    }

    @Test
    void handlerCanModifyPower() {
        BattleEventBus bus = new BattleEventBus();
        bus.registerHandler(BattleEventType.ON_BEFORE_MOVE, new BattleEventHandler() {
            @Override public EventResult handle(BattleEvent event, Map<String, Object> context) {
                if (event instanceof ModifyPowerEvent pwr) {
                    pwr.setModifiedPower((int) (pwr.getBasePower() * 1.5));
                }
                return EventResult.CONTINUE;
            }
            @Override public String getSource() { return "test:power-boost"; }
        });
        bus.freeze();

        ModifyPowerEvent pwrEvent = new ModifyPowerEvent(100);
        bus.fireEvent(BattleEventType.ON_BEFORE_MOVE, pwrEvent, Map.of());
        assertEquals(150, pwrEvent.getModifiedPower());
    }

    @Test
    void handlerCanModifyDamage() {
        BattleEventBus bus = new BattleEventBus();
        bus.registerHandler(BattleEventType.ON_DAMAGE, new BattleEventHandler() {
            @Override public EventResult handle(BattleEvent event, Map<String, Object> context) {
                if (event instanceof DamageEvent dmg) {
                    return EventResult.modifyAndContinue((int) (dmg.getFinalDamage() * 0.5));
                }
                return EventResult.CONTINUE;
            }
            @Override public String getSource() { return "test:dmg-reduce"; }
        });
        bus.freeze();

        DamageEvent dmgEvent = new DamageEvent(200, 200, false);
        EventResult result = bus.fireEvent(BattleEventType.ON_DAMAGE, dmgEvent, Map.of());
        assertTrue(result.isModified());
        assertEquals(100, (int) result.getModifiedValue());
    }

    @Test
    void handlerCanSetImmunity() {
        BattleEventBus bus = new BattleEventBus();
        bus.registerHandler(BattleEventType.ON_HIT, new BattleEventHandler() {
            @Override public EventResult handle(BattleEvent event, Map<String, Object> context) {
                return EventResult.IMMUNE;
            }
            @Override public String getSource() { return "test:immune"; }
        });
        bus.freeze();

        EventResult result = bus.fireEvent(BattleEventType.ON_HIT, TryHitEvent.normal(), Map.of());
        assertTrue(result.isImmune());
    }

    @Test
    void handlersAreExecutedByPriority() {
        BattleEventBus bus = new BattleEventBus();
        StringBuilder order = new StringBuilder();

        bus.registerHandler(BattleEventType.ON_BEFORE_MOVE, new BattleEventHandler() {
            @Override public EventResult handle(BattleEvent event, Map<String, Object> context) {
                order.append("B");
                return EventResult.CONTINUE;
            }
            @Override public int getPriority() { return 0; }
            @Override public String getSource() { return "test:low"; }
        });
        bus.registerHandler(BattleEventType.ON_BEFORE_MOVE, new BattleEventHandler() {
            @Override public EventResult handle(BattleEvent event, Map<String, Object> context) {
                order.append("A");
                return EventResult.CONTINUE;
            }
            @Override public int getPriority() { return 100; }
            @Override public String getSource() { return "test:high"; }
        });
        bus.freeze();

        bus.fireEvent(BattleEventType.ON_BEFORE_MOVE, new ModifyPowerEvent(50), Map.of());
        assertEquals("AB", order.toString(), "高优先级先执行");
    }

    @Test
    void frozenBusRejectsNewHandlers() {
        BattleEventBus bus = new BattleEventBus();
        bus.freeze();
        assertThrows(IllegalStateException.class, () ->
            bus.registerHandler(BattleEventType.ON_TURN_START, (event, context) -> EventResult.CONTINUE));
    }

    @Test
    void appliesToFilter_canSkipHandler() {
        BattleEventBus bus = new BattleEventBus();
        bus.registerHandler(BattleEventType.ON_HIT, new BattleEventHandler() {
            @Override public EventResult handle(BattleEvent event, Map<String, Object> context) {
                return EventResult.IMMUNE;
            }
            @Override public boolean appliesTo(BattleEvent event, Map<String, Object> context) {
                return "ghost".equals(context.get("type"));
            }
            @Override public String getSource() { return "test:conditional"; }
        });
        bus.freeze();

        // Should NOT trigger (type mismatch)
        EventResult result1 = bus.fireEvent(BattleEventType.ON_HIT, TryHitEvent.normal(), Map.of("type", "normal"));
        assertTrue(result1.shouldContinue());

        // Should trigger
        EventResult result2 = bus.fireEvent(BattleEventType.ON_HIT, TryHitEvent.normal(), Map.of("type", "ghost"));
        assertTrue(result2.isImmune());
    }

    @Test
    void getHandlerCount_returnsCorrectCount() {
        BattleEventBus bus = new BattleEventBus();
        bus.registerHandler(BattleEventType.ON_SET_STATUS, (e, ctx) -> EventResult.CONTINUE);
        bus.registerHandler(BattleEventType.ON_SET_STATUS, (e, ctx) -> EventResult.CONTINUE);
        bus.freeze();
        assertEquals(2, bus.getHandlerCount(BattleEventType.ON_SET_STATUS));
        assertEquals(0, bus.getHandlerCount(BattleEventType.ON_TURN_START));
    }

    @Test
    void eventResult_continueWithModifiedValue() {
        EventResult result = EventResult.modifyAndContinue(42);
        assertTrue(result.shouldContinue());
        assertTrue(result.isModified());
        assertEquals(42, (int) result.getModifiedValue());
    }

    @Test
    void eventResult_stopWithMessage() {
        EventResult result = EventResult.stopWithMessage("immune", "免疫了");
        assertFalse(result.shouldContinue());
        assertEquals("immune", result.getEffectType());
        assertEquals("免疫了", result.getMessage());
    }
}
