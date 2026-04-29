package com.lio9.battle.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BattleStateSupport {

    BattleStateSupport() {
    }

    Map<String, Object> cloneState(Map<String, Object> state) {
        // 采用手动递归深拷贝，避免 Jackson 在处理复杂嵌套 Map（如 team, volatiles）时可能出现的类型擦除或引用共享
        return deepCloneMap(state);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCloneMap(Map<String, Object> source) {
        if (source == null)
            return null;
        Map<String, Object> target = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                target.put(key, deepCloneMap((Map<String, Object>) value));
            } else if (value instanceof List) {
                target.put(key, deepCloneList((List<Object>) value));
            } else {
                // Number, String, Boolean 等不可变对象可以直接复用引用
                target.put(key, value);
            }
        }
        return target;
    }

    @SuppressWarnings("unchecked")
    private List<Object> deepCloneList(List<Object> source) {
        if (source == null)
            return null;
        List<Object> target = new ArrayList<>();
        for (Object item : source) {
            if (item instanceof Map) {
                target.add(deepCloneMap((Map<String, Object>) item));
            } else if (item instanceof List) {
                target.add(deepCloneList((List<Object>) item));
            } else {
                target.add(item);
            }
        }
        return target;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> castList(Object value) {
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    Map<String, Object> cloneMap(Map<String, Object> value) {
        return new LinkedHashMap<>(value == null ? Map.of() : value);
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> team(Map<String, Object> state, boolean player) {
        Object value = state.get(player ? "playerTeam" : "opponentTeam");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> roster(Map<String, Object> state, boolean player) {
        Object value = state.get(player ? "playerRoster" : "opponentRoster");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> rounds(Map<String, Object> state) {
        Object value = state.get("rounds");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        List<Map<String, Object>> created = new ArrayList<>();
        state.put("rounds", created);
        return created;
    }

    List<Integer> activeSlots(Map<String, Object> state, boolean player) {
        Object value = state.get(player ? "playerActiveSlots" : "opponentActiveSlots");
        if (value instanceof List<?> list) {
            List<Integer> slots = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number number) {
                    slots.add(number.intValue());
                }
            }
            return slots;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> moves(Map<String, Object> mon) {
        Object value = mon.get("moves");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> cooldowns(Map<String, Object> mon) {
        Object value = mon.get("cooldowns");
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        Map<String, Object> created = new LinkedHashMap<>();
        mon.put("cooldowns", created);
        return created;
    }
}