package com.lio9.battle.engine.event;

import com.lio9.battle.effect.MoveUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 特性事件处理器配置。
 *
 * <p>将 Showdown 特性效果以事件驱动方式注册到 {@link BattleEventBus}。
 * 注意：当前引擎主路径仍通过 {@link com.lio9.battle.effect.EffectRegistry} 派发，
 * 事件总线仅作为辅助/渐进迁移路径。</p>
 */
@Configuration
public class AbilityEventConfig {

    @Bean
    public BattleEventHandler levitateHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                if (event instanceof TryHitEvent) {
                    Map<String, Object> target = event.getTarget(context);
                    Map<String, Object> move = event.getMove(context);
                    if (target != null && move != null && "levitate".equalsIgnoreCase(getAbilityName(target))) {
                        if (toInt(move.get("type_id"), 0) == 4) { // GROUND
                            return EventResult.stopWithMessage("immune",
                                    target.get("name") + " floats with Levitate, immune to Ground");
                        }
                    }
                }
                return EventResult.CONTINUE;
            }

            @Override
            public int getPriority() { return 100; }

            @Override
            public String getSource() { return "ability:levitate"; }
        };
    }

    @Bean
    public BattleEventHandler technicianHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                if (event instanceof ModifyPowerEvent pe) {
                    Map<String, Object> source = event.getSource(context);
                    if (source != null && "technician".equalsIgnoreCase(getAbilityName(source))
                            && pe.getBasePower() > 0 && pe.getBasePower() <= 60) {
                        return EventResult.modifyAndContinue((int) Math.floor(pe.getBasePower() * 1.5));
                    }
                }
                return EventResult.CONTINUE;
            }

            @Override
            public int getPriority() { return 30; }

            @Override
            public String getSource() { return "ability:technician"; }
        };
    }

    @Bean
    public BattleEventHandler hugePowerHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                return EventResult.CONTINUE;
            }

            @Override
            public String getSource() { return "ability:huge-power"; }
        };
    }

    @Bean
    public BattleEventHandler adaptabilityHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                return EventResult.CONTINUE;
            }

            @Override
            public String getSource() { return "ability:adaptability"; }
        };
    }

    @Bean
    public BattleEventHandler toughClawsHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                if (event instanceof ModifyPowerEvent pe) {
                    Map<String, Object> source = event.getSource(context);
                    Map<String, Object> move = event.getMove(context);
                    if (source != null && move != null) {
                        String ability = getAbilityName(source);
                        if (("tough-claws".equalsIgnoreCase(ability) || "iron-fist".equalsIgnoreCase(ability))
                                && isContactMove(move)) {
                            return EventResult.modifyAndContinue((int) Math.floor(pe.getBasePower() * 1.3));
                        }
                    }
                }
                return EventResult.CONTINUE;
            }

            @Override
            public int getPriority() { return 30; }

            @Override
            public String getSource() { return "ability:tough-claws"; }
        };
    }

    @Bean
    public BattleEventHandler intimidateHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                if (event.getType() == BattleEventType.ON_SWITCH_IN) {
                    Map<String, Object> source = event.getSource(context);
                    if (source != null && "intimidate".equalsIgnoreCase(getAbilityName(source))) {
                        return EventResult.continueWith("intimidate");
                    }
                }
                return EventResult.CONTINUE;
            }

            @Override
            public String getSource() { return "ability:intimidate"; }
        };
    }

    // ── 辅助方法 ──────────────────────────────────────────────────────────

    private String getAbilityName(Map<String, Object> pokemon) {
        Object ability = pokemon.get("ability");
        if (ability instanceof Map<?, ?> m) {
            Object nameEn = m.get("name_en");
            if (nameEn != null) return String.valueOf(nameEn);
        }
        return ability == null ? "" : String.valueOf(ability);
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        if (value != null) try { return Integer.parseInt(value.toString()); } catch (NumberFormatException ignored) {}
        return fallback;
    }

    /** 使用招式 flags 判断是否为接触类招式 */
    private boolean isContactMove(Map<String, Object> move) {
        return MoveUtils.hasMoveFlag(move, "contact");
    }
}
