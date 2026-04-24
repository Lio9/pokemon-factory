package com.lio9.battle.engine.event;

import com.lio9.battle.engine.MoveRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 特性事件处理器配置
 * 将 Pokemon Showdown 中的特性效果注册到事件系统
 */
@Configuration
public class AbilityEventConfig {

    /**
     * 漂浮特性 - 免疫地面系招式
     */
    @Bean
    public BattleEventHandler levitateHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                if (event instanceof TryHitEvent) {
                    Map<String, Object> target = event.getTarget(context);
                    Map<String, Object> move = event.getMove(context);
                    
                    if (target != null && move != null) {
                        String ability = getAbilityName(target);
                        if ("levitate".equalsIgnoreCase(ability)) {
                            int moveTypeId = toInt(move.get("type_id"), 0);
                            if (moveTypeId == 4) { // TYPE_GROUND = 4
                                return EventResult.stopWithMessage("immune", 
                                    target.get("name") + " 漂浮在空中，免疫地面系招式");
                            }
                        }
                    }
                }
                return EventResult.CONTINUE;
            }

            @Override
            public int getPriority() {
                return 100; // 高优先级，免疫效果应该先判定
            }

            @Override
            public String getSource() {
                return "ability:levitate";
            }
        };
    }

    /**
     * 技术高手特性 - 威力60及以下的招式威力×1.5
     */
    @Bean
    public BattleEventHandler technicianHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                if (event instanceof ModifyPowerEvent powerEvent) {
                    Map<String, Object> source = event.getSource(context);
                    if (source != null) {
                        String ability = getAbilityName(source);
                        if ("technician".equalsIgnoreCase(ability)) {
                            if (powerEvent.getBasePower() > 0 && powerEvent.getBasePower() <= 60) {
                                int boosted = (int) Math.floor(powerEvent.getBasePower() * 1.5);
                                return EventResult.modifyAndContinue(boosted);
                            }
                        }
                    }
                }
                return EventResult.CONTINUE;
            }

            @Override
            public int getPriority() {
                return 30; // 与 Pokemon Showdown 保持一致
            }

            @Override
            public String getSource() {
                return "ability:technician";
            }
        };
    }

    /**
     * 大力士/瑜伽之力特性 - 攻击×2
     * 注意：这个特性在伤害计算中通过 abilityDamageModifier 处理
     * 这里注册用于其他可能的触发点
     */
    @Bean
    public BattleEventHandler hugePowerHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                // Huge Power 主要在伤害计算时生效
                // 这里可以用于日志记录或其他触发
                return EventResult.CONTINUE;
            }

            @Override
            public String getSource() {
                return "ability:huge-power";
            }
        };
    }

    /**
     * 适应力特性 - STAB 从 1.5 倍提升到 2 倍
     */
    @Bean
    public BattleEventHandler adaptabilityHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                // Adaptability 在 STAB 计算时检查
                return EventResult.CONTINUE;
            }

            @Override
            public String getSource() {
                return "ability:adaptability";
            }
        };
    }

    /**
     * 硬爪/钢拳特性 - 接触类招式威力×1.3
     */
    @Bean
    public BattleEventHandler toughClawsHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                if (event instanceof ModifyPowerEvent powerEvent) {
                    Map<String, Object> source = event.getSource(context);
                    Map<String, Object> move = event.getMove(context);
                    
                    if (source != null && move != null) {
                        String ability = getAbilityName(source);
                        if (("tough-claws".equalsIgnoreCase(ability) || 
                             "iron-fist".equalsIgnoreCase(ability)) &&
                            isContactMove(move)) {
                            int boosted = (int) Math.floor(powerEvent.getBasePower() * 1.3);
                            return EventResult.modifyAndContinue(boosted);
                        }
                    }
                }
                return EventResult.CONTINUE;
            }

            @Override
            public int getPriority() {
                return 30;
            }

            @Override
            public String getSource() {
                return "ability:tough-claws";
            }
        };
    }

    /**
     * 威吓特性 - 上场时降低对手攻击1级
     */
    @Bean
    public BattleEventHandler intimidateHandler() {
        return new BattleEventHandler() {
            @Override
            public EventResult handle(BattleEvent event, Map<String, Object> context) {
                if (event.getType() == BattleEventType.ON_SWITCH_IN) {
                    Map<String, Object> source = event.getSource(context);
                    if (source != null) {
                        String ability = getAbilityName(source);
                        if ("intimidate".equalsIgnoreCase(ability)) {
                            // 触发威吓效果（在对战流程中处理）
                            return EventResult.continueWith("intimidate");
                        }
                    }
                }
                return EventResult.CONTINUE;
            }

            @Override
            public String getSource() {
                return "ability:intimidate";
            }
        };
    }

    // === 辅助方法 ===

    private String getAbilityName(Map<String, Object> pokemon) {
        Object ability = pokemon.get("ability");
        if (ability instanceof Map<?, ?> abilityMap) {
            Object nameEn = abilityMap.get("name_en");
            if (nameEn != null && !String.valueOf(nameEn).isBlank()) {
                return String.valueOf(nameEn);
            }
        }
        return ability == null ? "" : String.valueOf(ability);
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private boolean isContactMove(Map<String, Object> move) {
        // 简化判断：物理招式且不是远程招式
        int damageClassId = toInt(move.get("damage_class_id"), 0);
        if (damageClassId != 2) { // DAMAGE_CLASS_PHYSICAL = 2
            return false;
        }
        
        // 检查是否为非接触类招式
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        return !nameEn.contains("beam") && !nameEn.contains("bomb") && 
               !nameEn.contains("pulse") && !nameEn.contains("wave");
    }
}
