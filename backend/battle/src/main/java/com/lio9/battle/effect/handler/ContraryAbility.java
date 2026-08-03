package com.lio9.battle.effect.handler;

import com.lio9.battle.effect.AbilityHandler;
import com.lio9.battle.effect.StatStageContext;

/**
 * 唱反调（Contrary）：能力阶级变化反转。
 *
 * <pre>
 * 注册：EffectRegistry.register(new ContraryAbility());
 * </pre>
 */
public class ContraryAbility implements AbilityHandler {

    @Override
    public String id() {
        return "contrary";
    }

    @Override
    public int onModifyStatStage(StatStageContext ctx, int delta) {
        // 反转能力阶级变化
        return -delta;
    }
}
