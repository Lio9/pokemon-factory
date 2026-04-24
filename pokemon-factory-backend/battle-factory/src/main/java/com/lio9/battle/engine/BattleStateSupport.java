package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BattleStateSupport {
    private final ObjectMapper mapper;

    BattleStateSupport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> cloneState(Map<String, Object> state) {
        try {
            return mapper.readValue(mapper.writeValueAsString(state), Map.class);
        } catch (Exception exception) {
            return new LinkedHashMap<>(state);
        }
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