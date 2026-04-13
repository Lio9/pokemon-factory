package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class BattlePreviewSupport {
    private final ObjectMapper mapper;
    private final BattleStateSupport stateSupport;
    private final int battleTeamSize;
    private final int activeSlots;

    BattlePreviewSupport(ObjectMapper mapper, BattleStateSupport stateSupport, int battleTeamSize, int activeSlots) {
        this.mapper = mapper;
        this.stateSupport = stateSupport;
        this.battleTeamSize = battleTeamSize;
        this.activeSlots = activeSlots;
    }

    Map<String, Object> autoSelect(List<Map<String, Object>> roster, long seed) {
        List<Integer> picked = new ArrayList<>();
        List<Map<String, Object>> sorted = new ArrayList<>();
        for (int index = 0; index < roster.size(); index++) {
            Map<String, Object> entry = new LinkedHashMap<>(roster.get(index));
            entry.put("rosterIndex", index);
            sorted.add(entry);
        }
        sorted.sort(Comparator.comparingInt((Map<String, Object> mon) -> toInt(mon.get("battleScore"), 0)).reversed()
                .thenComparingInt(mon -> toInt(stateSupport.castMap(mon.get("stats")).get("speed"), 0)).reversed());
        for (int index = 0; index < Math.min(battleTeamSize, sorted.size()); index++) {
            picked.add(toInt(sorted.get(index).get("rosterIndex"), index));
        }

        Map<String, Object> selection = new LinkedHashMap<>();
        selection.put("pickedRosterIndexes", picked);

        List<Integer> leads = new ArrayList<>();
        List<Integer> bySpeed = new ArrayList<>(picked);
        bySpeed.sort(Comparator.comparingInt((Integer index) -> toInt(stateSupport.castMap(roster.get(index).get("stats")).get("speed"), 0)).reversed());
        for (int index = 0; index < Math.min(activeSlots, bySpeed.size()); index++) {
            leads.add(bySpeed.get(index));
        }
        if (leads.size() < activeSlots && !picked.isEmpty()) {
            List<Integer> shuffled = new ArrayList<>(picked);
            Collections.shuffle(shuffled, new Random(seed));
            for (Integer candidate : shuffled) {
                if (!leads.contains(candidate)) {
                    leads.add(candidate);
                    if (leads.size() >= activeSlots) {
                        break;
                    }
                }
            }
        }
        selection.put("leadRosterIndexes", leads);
        return selection;
    }

    Map<String, Object> normalizeSelection(Map<String, Object> rawSelection, List<Map<String, Object>> roster, long seed) {
        List<Integer> picked = uniqueIndexes(rawSelection == null ? null : rawSelection.get("pickedRosterIndexes"));
        List<Integer> leads = uniqueIndexes(rawSelection == null ? null : rawSelection.get("leadRosterIndexes"));

        boolean validPicked = picked.size() == Math.min(battleTeamSize, roster.size())
                && picked.stream().allMatch(index -> index >= 0 && index < roster.size());
        boolean validLeads = leads.size() == Math.min(activeSlots, picked.size())
                && leads.stream().allMatch(picked::contains);
        if (!validPicked || !validLeads) {
            return autoSelect(roster, seed);
        }

        Map<String, Object> selection = new LinkedHashMap<>();
        selection.put("pickedRosterIndexes", picked);
        selection.put("leadRosterIndexes", leads);
        return selection;
    }

    List<Map<String, Object>> buildBattleTeam(List<Map<String, Object>> roster, Map<String, Object> selection) {
        List<Integer> picked = uniqueIndexes(selection.get("pickedRosterIndexes"));
        List<Integer> leads = uniqueIndexes(selection.get("leadRosterIndexes"));
        List<Integer> ordered = new ArrayList<>(leads);
        for (Integer index : picked) {
            if (!ordered.contains(index)) {
                ordered.add(index);
            }
        }

        List<Map<String, Object>> selected = new ArrayList<>();
        for (Integer rosterIndex : ordered) {
            if (rosterIndex == null || rosterIndex < 0 || rosterIndex >= roster.size()) {
                continue;
            }
            Map<String, Object> mon = normalizePokemon(roster.get(rosterIndex));
            mon.put("rosterIndex", rosterIndex);
            mon.put("currentHp", toInt(stateSupport.castMap(mon.get("stats")).get("hp"), 1));
            mon.put("status", "ready");
            mon.put("condition", null);
            mon.put("cooldowns", new LinkedHashMap<>());
            mon.put("entryRound", 1);
            mon.put("flinched", false);
            mon.put("sleepTurns", 0);
            mon.put("sleepAppliedRound", 0);
            mon.put("tauntTurns", 0);
            mon.putIfAbsent("itemConsumed", false);
            mon.putIfAbsent("choiceLockedMove", null);
            selected.add(mon);
        }
        return selected;
    }

    List<Integer> initialActiveSlots(List<Map<String, Object>> team) {
        List<Integer> slots = new ArrayList<>();
        for (int index = 0; index < team.size() && slots.size() < activeSlots; index++) {
            if (toInt(team.get(index).get("currentHp"), 0) > 0) {
                slots.add(index);
            }
        }
        return slots;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> parseTeam(String teamJson) {
        if (teamJson == null || teamJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            Object parsed = mapper.readValue(teamJson, Object.class);
            if (parsed instanceof List) {
                return (List<Map<String, Object>>) parsed;
            }
            if (parsed instanceof Map) {
                return new ArrayList<>(List.of((Map<String, Object>) parsed));
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    List<Map<String, Object>> normalizeRoster(List<Map<String, Object>> roster) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> pokemon : roster) {
            normalized.add(normalizePokemon(pokemon));
        }
        return normalized;
    }

    Map<String, Object> normalizePokemon(Map<String, Object> pokemon) {
        Map<String, Object> normalized = stateSupport.cloneMap(pokemon);
        normalized.put("stats", stateSupport.castMap(normalized.get("stats")));
        normalized.put("types", stateSupport.castList(normalized.get("types")));
        normalized.put("moves", normalizeMoves(stateSupport.castList(normalized.get("moves"))));
        normalized.putIfAbsent("currentHp", toInt(stateSupport.castMap(normalized.get("stats")).get("hp"), 1));
        normalized.putIfAbsent("cooldowns", new LinkedHashMap<>());
        normalized.putIfAbsent("statStages", new LinkedHashMap<>(Map.of("attack", 0, "specialAttack", 0, "speed", 0)));
        normalized.putIfAbsent("condition", null);
        normalized.putIfAbsent("entryRound", 1);
        normalized.putIfAbsent("flinched", false);
        normalized.putIfAbsent("sleepTurns", 0);
        normalized.putIfAbsent("sleepAppliedRound", 0);
        normalized.putIfAbsent("tauntTurns", 0);
        normalized.putIfAbsent("itemConsumed", false);
        normalized.putIfAbsent("choiceLockedMove", null);
        return normalized;
    }

    List<Map<String, Object>> normalizeMoves(List<Map<String, Object>> moves) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> move : moves) {
            Map<String, Object> copied = stateSupport.cloneMap(move);
            copied.put("power", toInt(copied.get("power"), 0));
            copied.put("accuracy", toInt(copied.get("accuracy"), 100));
            copied.put("priority", toInt(copied.get("priority"), 0));
            copied.put("damage_class_id", toInt(copied.get("damage_class_id"), 0));
            copied.put("type_id", toInt(copied.get("type_id"), 0));
            copied.put("target_id", toInt(copied.get("target_id"), 0));
            normalized.add(copied);
        }
        return normalized;
    }

    private List<Integer> uniqueIndexes(Object value) {
        Set<Integer> indexes = new LinkedHashSet<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number number) {
                    indexes.add(number.intValue());
                } else if (item != null) {
                    try {
                        indexes.add(Integer.parseInt(item.toString()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return new ArrayList<>(indexes);
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
}