package com.lio9.battle.engine;

/**
 * BattlePreviewSupport 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战引擎文件。
 * 核心职责：负责 BattlePreviewSupport 所在的对战规则拆分逻辑，用于从主引擎中拆出独立的规则处理职责。
 * 阅读建议：建议先理解该文件的入口方法，再回看 BattleEngine 中的调用位置。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
    /**
     * 队伍预览与初始战斗数据标准化支持类。
     * <p>
     * 其职责不是推进回合，而是把前端/数据库给出的原始宝可梦数据整理成战斗引擎可直接消费的统一结构。
     * 本轮额外负责在建队阶段预种 control volatiles，保证新旧状态字段在开局时就保持一致。
     * </p>
     */
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
        bySpeed.sort(Comparator
                .comparingInt(
                        (Integer index) -> toInt(stateSupport.castMap(roster.get(index).get("stats")).get("speed"), 0))
                .reversed());
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

    Map<String, Object> normalizeSelection(Map<String, Object> rawSelection, List<Map<String, Object>> roster,
            long seed) {
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
            mon.put("confused", false);
            mon.put("confusionTurns", 0);
            mon.put("sleepTurns", 0);
            mon.put("sleepAppliedRound", 0);
            mon.put("yawnTurns", 0);
            mon.put("toxicCounter", 0);
            mon.put("tauntTurns", 0);
            mon.put("healBlockTurns", 0);
            mon.put("tormentTurns", 0);
            mon.put("disableTurns", 0);
            mon.put("disableMove", null);
            mon.put("encoreTurns", 0);
            mon.put("encoreMove", null);
            mon.put("lastMoveUsed", null);
            mon.put("rechargeTurns", 0);
            mon.put("chargingMove", null);
            mon.put("chargingTurns", 0);
            mon.put("chargingTargetTeamIndex", -1);
            mon.put("chargingTargetFieldSlot", -1);
            mon.put("protectionStreak", 0);
            mon.put("lastProtectionRound", 0);
            mon.put("specialSystemActivated", null);
            mon.put("terastallized", false);
            mon.put("megaEvolved", false);
            mon.put("dynamaxed", false);
            mon.put("dynamaxTurnsRemaining", 0);
            mon.put("dynamaxBaseHp", toInt(stateSupport.castMap(mon.get("stats")).get("hp"), 1));
            mon.put("zMoveUsed", false);
            mon.putIfAbsent("itemConsumed", false);
            mon.putIfAbsent("choiceLockedMove", null);
            mon.put("volatiles", new LinkedHashMap<>());
            // 新战斗状态统一预创建 volatile 容器，避免后续首次访问时才补齐导致测试快照不稳定。
            seedControlVolatiles(mon);
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

    List<Map<String, Object>> parseTeam(String teamJson) {
        if (teamJson == null || teamJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            Object parsed = mapper.readValue(teamJson, Object.class);
            if (parsed instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> result = (List<Map<String, Object>>) parsed;
                return result;
            }
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> singlePokemon = (Map<String, Object>) parsed;
                return new ArrayList<>(List.of(singlePokemon));
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
        Map<String, Object> teraType = normalizeTeraType(normalized);
        normalized.put("teraType", teraType);
        normalized.put("teraTypeId", toInt(teraType.get("type_id"), 0));
        normalized.putIfAbsent("currentHp", toInt(stateSupport.castMap(normalized.get("stats")).get("hp"), 1));
        normalized.putIfAbsent("cooldowns", new LinkedHashMap<>());
        normalized.putIfAbsent("statStages", new LinkedHashMap<>(Map.of(
                "attack", 0,
                "defense", 0,
                "specialAttack", 0,
                "specialDefense", 0,
                "speed", 0)));
        normalized.putIfAbsent("condition", null);
        normalized.putIfAbsent("entryRound", 1);
        normalized.putIfAbsent("flinched", false);
        normalized.putIfAbsent("confused", false);
        normalized.putIfAbsent("confusionTurns", 0);
        normalized.putIfAbsent("sleepTurns", 0);
        normalized.putIfAbsent("sleepAppliedRound", 0);
        normalized.putIfAbsent("yawnTurns", 0);
        normalized.putIfAbsent("toxicCounter", 0);
        normalized.putIfAbsent("tauntTurns", 0);
        normalized.putIfAbsent("healBlockTurns", 0);
        normalized.putIfAbsent("tormentTurns", 0);
        normalized.putIfAbsent("disableTurns", 0);
        normalized.putIfAbsent("disableMove", null);
        normalized.putIfAbsent("encoreTurns", 0);
        normalized.putIfAbsent("encoreMove", null);
        normalized.putIfAbsent("lastMoveUsed", null);
        normalized.putIfAbsent("protectionStreak", 0);
        normalized.putIfAbsent("lastProtectionRound", 0);
        normalized.putIfAbsent("specialSystemActivated", null);
        normalized.putIfAbsent("terastallized", false);
        normalized.putIfAbsent("megaEvolved", false);
        normalized.putIfAbsent("dynamaxed", false);
        normalized.putIfAbsent("dynamaxTurnsRemaining", 0);
        normalized.putIfAbsent("dynamaxBaseHp", toInt(stateSupport.castMap(normalized.get("stats")).get("hp"), 1));
        normalized.putIfAbsent("zMoveUsed", false);
        normalized.put("megaEligible", inferMegaEligibility(normalized));
        normalized.put("zMoveEligible", inferZMoveEligibility(normalized));
        normalized.put("dynamaxEligible", inferDynamaxEligibility(normalized));
        normalized.put("specialSystems", inferSpecialSystems(normalized));
        normalized.putIfAbsent("itemConsumed", false);
        normalized.putIfAbsent("choiceLockedMove", null);
        normalized.putIfAbsent("volatiles", new LinkedHashMap<>());
        // 对历史数据/测试数据做兼容：即使原对象只带旧字段，也在标准化阶段补出对应 volatile。
        seedControlVolatiles(normalized);
        return normalized;
    }

    private void seedControlVolatiles(Map<String, Object> mon) {
        Map<String, Object> volatiles = stateSupport.castMap(mon.get("volatiles"));
        // 控制类状态统一放入 volatile，旧字段继续保留用于兼容。
        // putIfAbsent 的意义是：如果上游已经显式传入了新的 volatile 值，就不再被旧字段覆盖。
        volatiles.putIfAbsent("tauntTurns", toInt(mon.get("tauntTurns"), 0));
        volatiles.putIfAbsent("healBlockTurns", toInt(mon.get("healBlockTurns"), 0));
        volatiles.putIfAbsent("tormentTurns", toInt(mon.get("tormentTurns"), 0));
        volatiles.putIfAbsent("disableTurns", toInt(mon.get("disableTurns"), 0));
        volatiles.putIfAbsent("disableMove", mon.get("disableMove"));
        volatiles.putIfAbsent("encoreTurns", toInt(mon.get("encoreTurns"), 0));
        volatiles.putIfAbsent("encoreMove", mon.get("encoreMove"));
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
            copied.put("min_hits", toInt(copied.get("min_hits"), 0));
            copied.put("max_hits", toInt(copied.get("max_hits"), 0));
            copied.put("crit_rate", toInt(copied.get("crit_rate"), 0));
            copied.put("effect_chance", toInt(copied.get("effect_chance"), 0));
            copied.put("ailment_chance", toInt(copied.get("ailment_chance"), 0));
            copied.put("flinch_chance", toInt(copied.get("flinch_chance"), 0));
            copied.put("stat_chance", toInt(copied.get("stat_chance"), 0));
            copied.put("drain", toInt(copied.get("drain"), 0));
            copied.put("healing", toInt(copied.get("healing"), 0));
            copied.put("ailment_name_en", copied.getOrDefault("ailment_name_en", ""));
            copied.put("category_name_en", copied.getOrDefault("category_name_en", ""));
            copied.put("effect_short", copied.getOrDefault("effect_short", ""));
            copied.put("flags", normalizeFlags(copied.get("flags")));
            copied.put("metaStatChanges",
                    normalizeStatChanges(copied.get("metaStatChanges"), copied.get("stat_changes")));
            normalized.add(copied);
        }
        return normalized;
    }

    private List<String> normalizeFlags(Object raw) {
        List<String> flags = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    flags.add(item.toString());
                }
            }
            return flags;
        }
        if (raw == null) {
            return flags;
        }
        String text = raw.toString().trim();
        if (text.isBlank()) {
            return flags;
        }
        for (String part : text.split(",")) {
            String value = part.trim();
            if (!value.isBlank()) {
                flags.add(value);
            }
        }
        return flags;
    }

    private List<Map<String, Object>> normalizeStatChanges(Object rawList, Object rawString) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> statChange : stateSupport.castList(rawList)) {
            normalized.add(Map.of(
                    "stat_id", toInt(statChange.get("stat_id"), 0),
                    "change", toInt(statChange.get("change"), 0)));
        }
        if (!normalized.isEmpty()) {
            return normalized;
        }
        if (rawString == null) {
            return normalized;
        }
        String text = rawString.toString().trim();
        if (text.isBlank()) {
            return normalized;
        }
        for (String part : text.split(",")) {
            String[] pair = part.trim().split(":");
            if (pair.length != 2) {
                continue;
            }
            normalized.add(Map.of(
                    "stat_id", toInt(pair[0], 0),
                    "change", toInt(pair[1], 0)));
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

    private Map<String, Object> normalizeTeraType(Map<String, Object> pokemon) {
        Object raw = pokemon.get("teraType");
        if (!(raw instanceof Map<?, ?>)) {
            raw = pokemon.get("tera_type");
        }
        if (raw instanceof Map<?, ?> teraMap) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            Object typeId = teraMap.get("type_id");
            if (typeId == null) {
                typeId = teraMap.get("id");
            }
            normalized.put("type_id", toInt(typeId, 0));
            Object name = teraMap.get("name");
            Object nameEn = teraMap.get("name_en");
            normalized.put("name", name == null ? "" : name);
            normalized.put("name_en", nameEn == null ? "" : nameEn);
            if (toInt(normalized.get("type_id"), 0) > 0) {
                return normalized;
            }
        }
        int teraTypeId = toInt(pokemon.get("teraTypeId"), toInt(pokemon.get("tera_type_id"), 0));
        for (Map<String, Object> type : stateSupport.castList(pokemon.get("types"))) {
            if (teraTypeId <= 0 || toInt(type.get("type_id"), 0) == teraTypeId) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                normalized.put("type_id", teraTypeId > 0 ? teraTypeId : toInt(type.get("type_id"), 0));
                normalized.put("name", type.getOrDefault("name", ""));
                normalized.put("name_en", type.getOrDefault("name_en", ""));
                return normalized;
            }
        }
        return new LinkedHashMap<>(Map.of("type_id", teraTypeId));
    }

    private List<String> inferSpecialSystems(Map<String, Object> pokemon) {
        List<String> systems = new ArrayList<>();
        if (toInt(pokemon.get("teraTypeId"), 0) > 0) {
            systems.add("tera");
        }
        if (Boolean.TRUE.equals(pokemon.get("megaEligible"))) {
            systems.add("mega");
        }
        if (Boolean.TRUE.equals(pokemon.get("zMoveEligible"))) {
            systems.add("z-move");
        }
        if (Boolean.TRUE.equals(pokemon.get("dynamaxEligible"))) {
            systems.add("dynamax");
        }
        return systems;
    }

    private boolean inferMegaEligibility(Map<String, Object> pokemon) {
        if (Boolean.TRUE.equals(pokemon.get("megaEligible"))) {
            return true;
        }
        return pokemon.containsKey("megaStats") || pokemon.containsKey("megaAbility")
                || pokemon.containsKey("megaTypes");
    }

    private boolean inferZMoveEligibility(Map<String, Object> pokemon) {
        if (Boolean.TRUE.equals(pokemon.get("zMoveEligible"))) {
            return true;
        }
        String heldItem = String.valueOf(pokemon.getOrDefault("heldItem", ""));
        return heldItem.endsWith("-z");
    }

    private boolean inferDynamaxEligibility(Map<String, Object> pokemon) {
        return Boolean.TRUE.equals(pokemon.get("dynamaxEligible"))
                || Boolean.TRUE.equals(pokemon.get("gigantamaxEligible"));
    }
}
