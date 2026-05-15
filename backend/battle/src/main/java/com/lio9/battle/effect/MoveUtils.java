package com.lio9.battle.effect;

import java.util.List;
import java.util.Map;

/**
 * 招式检测与通用 Map 取值工具方法。
 * <p>从 EffectRegistry 拆分，保持行为一致。</p>
 */
public final class MoveUtils {

    private MoveUtils() {}

    public static boolean hasStatus(String condition) {
        return condition != null && !condition.isEmpty()
                && !"ready".equals(condition) && !"fainted".equals(condition);
    }

    public static boolean isSpecies(Map<String, Object> mon, String... names) {
        String s = strVal(mon, "name_en");
        for (String n : names) if (n.equalsIgnoreCase(s)) return true;
        return false;
    }

    public static boolean isSlicingMove(Map<String, Object> move) {
        String n = moveName(move);
        return n.contains("slash") || n.contains("cut") || n.contains("blade")
            || n.contains("razor") || n.contains("claw") || n.contains("axe")
            || n.contains("night-slash") || n.contains("night slash")
            || n.contains("psycho-cut") || n.contains("leaf-blade");
    }

    public static boolean isSoundMove(Map<String, Object> move) {
        String n = moveName(move);
        return n.contains("boomburst") || n.contains("hypervoice") || n.contains("hyper voice")
            || n.contains("bug-buzz") || n.contains("bug buzz")
            || n.contains("snarl") || n.contains("overdrive") || n.contains("clang");
    }

    public static boolean isWindMove(Map<String, Object> move) {
        String n = moveName(move);
        return n.contains("gust") || n.contains("twister") || n.contains("hurricane")
            || n.contains("bleakwind") || n.contains("icy-wind") || n.contains("icy wind")
            || n.contains("heat-wave") || n.contains("heat wave")
            || n.contains("tailwind") || n.contains("air-slash");
    }

    public static boolean isRecoilMove(Map<String, Object> move) {
        String n = moveName(move);
        return n.contains("double-edge") || n.contains("double edge")
            || n.contains("flare-blitz") || n.contains("flare blitz")
            || n.contains("wood-hammer") || n.contains("wood hammer")
            || n.contains("head-smash") || n.contains("head smash")
            || n.contains("brave-bird") || n.contains("brave bird")
            || n.contains("wild-charge") || n.contains("wild charge")
            || n.contains("volt-tackle") || n.contains("volt tackle");
    }

    public static boolean moveCategory(String cat, Map<String, Object> move) {
        return moveName(move).contains(cat);
    }

    public static boolean hasEffectChance(Map<String, Object> move) {
        return intVal(move, "effect_chance") > 0;
    }

    @SuppressWarnings("unchecked")
    public static boolean hasMoveFlag(Map<String, Object> move, String expected) {
        Object flags = move.get("flags");
        if (flags instanceof List<?> list) {
            for (Object flag : list) {
                if (expected.equalsIgnoreCase(String.valueOf(flag))) return true;
            }
            return false;
        }
        if (flags instanceof String s) {
            for (String part : s.split(",")) {
                if (expected.equalsIgnoreCase(part.trim())) return true;
            }
        }
        return false;
    }

    public static String moveName(Map<String, Object> move) {
        Object n = move.get("name_en");
        return n == null ? "" : String.valueOf(n).toLowerCase();
    }

    public static int intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    public static String strVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : String.valueOf(v).toLowerCase();
    }
}
