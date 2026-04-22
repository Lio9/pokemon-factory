package com.lio9.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.BattleDexMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 对战工厂 AI 队伍生成服务。
 * <p>
 * 负责从图鉴公共表中挑选候选宝可梦、构造可对战的队伍 JSON，
 * 并补齐招式、能力、道具、努力值和战斗属性等信息。
 * </p>
 */
@Service
public class AIService {
    private static final int LEVEL = 50;
    private static final List<String> ITEM_POOL = List.of(
            "focus-sash",
            "sitrus-berry",
            "assault-vest",
            "life-orb",
            "choice-scarf",
            "choice-band",
            "choice-specs",
            "clear-amulet",
            "covert-cloak",
            "mental-herb",
            "safety-goggles",
            "leftovers",
            "rocky-helmet",
            "light-clay",
            "terrain-extender",
            "mystic-water",
            "charcoal",
            "miracle-seed"
    );

    private final BattleDexMapper battleDexMapper;
    private final ObjectMapper mapper;

    /**
     * 注入 battleFactory 侧使用的图鉴查询能力。
     */
    public AIService(BattleDexMapper battleDexMapper, ObjectMapper mapper) {
        this.battleDexMapper = battleDexMapper;
        this.mapper = mapper;
    }

    /**
     * 判断队伍 JSON 是否为空。
     */
    public boolean isBlankTeamJson(String teamJson) {
        return teamJson == null || teamJson.isBlank() || "[]".equals(teamJson.trim());
    }

    /**
     * 生成一支对战工厂使用的标准队伍 JSON。
     */
    public String generateFactoryTeamJson(int size, int rank, long seed, Set<String> excludedNames) {
        int normalizedSize = Math.max(4, Math.min(6, size));
        List<Map<String, Object>> candidates = battleDexMapper.selectRandomDefaultForms(normalizedSize * 10);
        Random random = new Random(seed);
        Set<String> excluded = normalizeNames(excludedNames);
        Set<String> usedSpecies = new LinkedHashSet<>();
        List<Map<String, Object>> roster = new ArrayList<>();
        List<String> itemPool = new ArrayList<>(ITEM_POOL);

        for (Map<String, Object> candidate : candidates) {
            if (roster.size() >= normalizedSize) {
                break;
            }

            String speciesName = String.valueOf(candidate.getOrDefault("name_en", candidate.getOrDefault("name", "unknown"))).toLowerCase();
            if (excluded.contains(speciesName) || usedSpecies.contains(speciesName)) {
                continue;
            }

            Map<String, Object> pokemon = buildCompetitivePokemon(candidate, random, itemPool);
            if (pokemon != null) {
                roster.add(pokemon);
                usedSpecies.add(speciesName);
            }
        }

        try {
            return mapper.writeValueAsString(roster);
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * 把队伍 JSON 解析成前后端共享的 List<Map> 结构。
     */
    public List<Map<String, Object>> parseTeam(String teamJson) {
        if (isBlankTeamJson(teamJson)) {
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

    /**
     * 提取队伍中已经使用过的物种名，用于避免镜像对局。
     */
    public Set<String> extractNames(String teamJson) {
        Set<String> names = new LinkedHashSet<>();
        for (Map<String, Object> pokemon : parseTeam(teamJson)) {
            Object name = pokemon.get("name_en");
            if (name == null) {
                name = pokemon.get("name");
            }
            if (name != null) {
                names.add(name.toString());
            }
        }
        return normalizeNames(names);
    }

    /**
     * 基于图鉴候选构造单只可参战宝可梦。
     */
    private Map<String, Object> buildCompetitivePokemon(Map<String, Object> candidate, Random random, List<String> itemPool) {
        Integer formId = toInt(candidate.get("form_id"), null);
        if (formId == null) {
            return null;
        }

        List<Map<String, Object>> stats = battleDexMapper.selectFormStats(formId);
        List<Map<String, Object>> types = battleDexMapper.selectFormTypes(formId);
        List<Map<String, Object>> abilities = battleDexMapper.selectFormAbilities(formId);
        List<Map<String, Object>> movePool = battleDexMapper.selectCompetitiveMoves(formId, 36);
        if (stats == null || stats.isEmpty() || types == null || types.isEmpty() || movePool == null || movePool.isEmpty()) {
            return null;
        }
        if (abilities == null) {
            abilities = new ArrayList<>();
        }

        Map<Integer, Integer> statMap = new LinkedHashMap<>();
        for (Map<String, Object> stat : stats) {
            statMap.put(toInt(stat.get("stat_id"), 0), toInt(stat.get("base_stat"), 50));
        }

        String build = statMap.getOrDefault(2, 50) >= statMap.getOrDefault(4, 50) ? "physical" : "special";
        List<Map<String, Object>> selectedMoves = pickMoves(movePool, build, random, statMap.getOrDefault(6, 80));
        if (selectedMoves.size() < 4) {
            return null;
        }

        Map<String, Object> pokemon = new LinkedHashMap<>(candidate);
        pokemon.put("level", LEVEL);
        pokemon.put("types", types);
        pokemon.put("moves", selectedMoves);
        pokemon.put("ability", abilities.isEmpty() ? null : abilities.get(0));
        pokemon.put("heldItem", pickUniqueItem(itemPool, random, selectedMoves));
        Map<String, Object> teraType = pickTeraType(types, selectedMoves, random);
        pokemon.put("teraType", teraType);
        pokemon.put("teraTypeId", toInt(teraType.get("type_id"), 0));
        pokemon.put("nature", "physical".equals(build) ? "adamant" : "modest");
        pokemon.put("evSpread", "physical".equals(build) ? Map.of("hp", 4, "atk", 252, "def", 0, "spa", 0, "spd", 0, "spe", 252) : Map.of("hp", 4, "atk", 0, "def", 0, "spa", 252, "spd", 0, "spe", 252));
        pokemon.put("stats", buildBattleStats(statMap, "physical".equals(build)));
        assignSpecialSystemProfile(pokemon, random);
        pokemon.put("battleScore", toInt(candidate.get("base_experience"), 50) + totalPower(selectedMoves) + toInt(candidate.get("base_experience"), 50));
        return pokemon;
    }

    private Map<String, Object> buildBattleStats(Map<Integer, Integer> baseStats, boolean physical) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("hp", calculateHp(baseStats.getOrDefault(1, 80), 31, 4, LEVEL));
        stats.put("attack", calculateOtherStat(baseStats.getOrDefault(2, 80), 31, physical ? 252 : 0, LEVEL, physical ? 1.1 : 0.9));
        stats.put("defense", calculateOtherStat(baseStats.getOrDefault(3, 80), 31, 0, LEVEL, 1.0));
        stats.put("specialAttack", calculateOtherStat(baseStats.getOrDefault(4, 80), 31, physical ? 0 : 252, LEVEL, physical ? 0.9 : 1.1));
        stats.put("specialDefense", calculateOtherStat(baseStats.getOrDefault(5, 80), 31, 0, LEVEL, 1.0));
        stats.put("speed", calculateOtherStat(baseStats.getOrDefault(6, 80), 31, 252, LEVEL, 1.0));
        return stats;
    }

    private List<Map<String, Object>> pickMoves(List<Map<String, Object>> movePool, String build, Random random, int baseSpeed) {
        List<Map<String, Object>> attacks = new ArrayList<>();
        Map<String, Object> protect = null;
        List<Map<String, Object>> utilities = new ArrayList<>();
        for (Map<String, Object> move : movePool) {
            String nameEn = String.valueOf(move.getOrDefault("name_en", ""));
            if ("protect".equalsIgnoreCase(nameEn)) {
                protect = normalizeMove(move);
                continue;
            }
            if ("tailwind".equalsIgnoreCase(nameEn)
                    || "trick-room".equalsIgnoreCase(nameEn)
                    || "trick room".equalsIgnoreCase(nameEn)
                    || "wide-guard".equalsIgnoreCase(nameEn)
                    || "wide guard".equalsIgnoreCase(nameEn)
                    || "quick-guard".equalsIgnoreCase(nameEn)
                    || "quick guard".equalsIgnoreCase(nameEn)
                    || "rain-dance".equalsIgnoreCase(nameEn)
                    || "rain dance".equalsIgnoreCase(nameEn)
                    || "sunny-day".equalsIgnoreCase(nameEn)
                    || "sunny day".equalsIgnoreCase(nameEn)
                    || "aurora-veil".equalsIgnoreCase(nameEn)
                    || "aurora veil".equalsIgnoreCase(nameEn)
                    || "electric-terrain".equalsIgnoreCase(nameEn)
                    || "electric terrain".equalsIgnoreCase(nameEn)
                    || "psychic-terrain".equalsIgnoreCase(nameEn)
                    || "psychic terrain".equalsIgnoreCase(nameEn)
                    || "reflect".equalsIgnoreCase(nameEn)
                    || "safeguard".equalsIgnoreCase(nameEn)
                    || "light-screen".equalsIgnoreCase(nameEn)
                    || "light screen".equalsIgnoreCase(nameEn)
                    || "taunt".equalsIgnoreCase(nameEn)
                    || "disable".equalsIgnoreCase(nameEn)
                    || "heal-block".equalsIgnoreCase(nameEn)
                    || "heal block".equalsIgnoreCase(nameEn)
                    || "torment".equalsIgnoreCase(nameEn)
                    || "encore".equalsIgnoreCase(nameEn)
                    || "yawn".equalsIgnoreCase(nameEn)
                    || "spore".equalsIgnoreCase(nameEn)
                    || "helping-hand".equalsIgnoreCase(nameEn)
                    || "helping hand".equalsIgnoreCase(nameEn)
                    || "ally-switch".equalsIgnoreCase(nameEn)
                    || "ally switch".equalsIgnoreCase(nameEn)
                    || "fake-tears".equalsIgnoreCase(nameEn)
                    || "fake tears".equalsIgnoreCase(nameEn)
                    || "parting-shot".equalsIgnoreCase(nameEn)
                    || "parting shot".equalsIgnoreCase(nameEn)
                    || "follow-me".equalsIgnoreCase(nameEn)
                    || "follow me".equalsIgnoreCase(nameEn)
                    || "rage-powder".equalsIgnoreCase(nameEn)
                    || "rage powder".equalsIgnoreCase(nameEn)
                    || "will-o-wisp".equalsIgnoreCase(nameEn)
                    || "will o wisp".equalsIgnoreCase(nameEn)
                    || "thunder-wave".equalsIgnoreCase(nameEn)
                    || "thunder wave".equalsIgnoreCase(nameEn)
                    || "icy-wind".equalsIgnoreCase(nameEn)
                    || "icy wind".equalsIgnoreCase(nameEn)) {
                utilities.add(normalizeMove(move));
                continue;
            }
            int damageClassId = toInt(move.get("damage_class_id"), 0);
            if (("physical".equals(build) && damageClassId == 1) || ("special".equals(build) && damageClassId == 2)) {
                attacks.add(normalizeMove(move));
            }
        }

        attacks.sort(Comparator.comparingInt((Map<String, Object> move) -> toInt(move.get("power"), 0)).reversed()
                .thenComparingInt(move -> toInt(move.get("accuracy"), 100)).reversed());

        List<Map<String, Object>> selected = new ArrayList<>();
        Map<String, Object> utility = preferredUtility(utilities, baseSpeed);
        int desiredAttacks = Math.max(1, 4 - (protect == null ? 0 : 1) - (utility == null ? 0 : 1));
        Set<Integer> usedTypes = new LinkedHashSet<>();
        for (Map<String, Object> move : attacks) {
            if (selected.size() >= desiredAttacks) {
                break;
            }
            int typeId = toInt(move.get("type_id"), 0);
            if (usedTypes.add(typeId) || selected.isEmpty()) {
                selected.add(move);
            }
        }

        for (Map<String, Object> move : attacks) {
            if (selected.size() >= desiredAttacks) {
                break;
            }
            if (selected.stream().noneMatch(existing -> existing.get("name_en").equals(move.get("name_en")))) {
                selected.add(move);
            }
        }

        if (utility != null) {
            selected.add(utility);
        }

        if (protect != null) {
            selected.add(protect);
        }

        List<Map<String, Object>> remaining = new ArrayList<>(attacks);
        remaining.removeIf(m -> selected.stream().anyMatch(s -> s.get("name_en").equals(m.get("name_en"))));
        while (selected.size() < 4 && !remaining.isEmpty()) {
            int idx = random.nextInt(remaining.size());
            selected.add(remaining.remove(idx));
        }

        return selected;
    }

    private Map<String, Object> pickTeraType(List<Map<String, Object>> types, List<Map<String, Object>> moves, Random random) {
        Map<Integer, Integer> attackWeights = new LinkedHashMap<>();
        for (Map<String, Object> move : moves) {
            if (toInt(move.get("power"), 0) <= 0) {
                continue;
            }
            int typeId = toInt(move.get("type_id"), 0);
            if (typeId <= 0) {
                continue;
            }
            attackWeights.put(typeId, attackWeights.getOrDefault(typeId, 0) + Math.max(1, toInt(move.get("power"), 0)));
        }
        Map<String, Object> preferred = null;
        int bestWeight = Integer.MIN_VALUE;
        for (Map<String, Object> type : types) {
            int typeId = toInt(type.get("type_id"), 0);
            int weight = attackWeights.getOrDefault(typeId, 0);
            if (weight > bestWeight) {
                bestWeight = weight;
                preferred = type;
            }
        }
        if (preferred != null) {
            return new LinkedHashMap<>(preferred);
        }
        return new LinkedHashMap<>(types.get(random.nextInt(types.size())));
    }

    private Map<String, Object> preferredUtility(List<Map<String, Object>> utilities, int baseSpeed) {
        if (utilities.isEmpty()) {
            return null;
        }
        if (baseSpeed <= 60) {
            for (Map<String, Object> move : utilities) {
                if ("trick-room".equalsIgnoreCase(String.valueOf(move.get("name_en"))) || "trick room".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                    return move;
                }
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("tailwind".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            String nameEn = String.valueOf(move.get("name_en"));
            if ("rain-dance".equalsIgnoreCase(nameEn)
                    || "rain dance".equalsIgnoreCase(nameEn)
                    || "sunny-day".equalsIgnoreCase(nameEn)
                    || "sunny day".equalsIgnoreCase(nameEn)
                    || "electric-terrain".equalsIgnoreCase(nameEn)
                    || "electric terrain".equalsIgnoreCase(nameEn)
                    || "psychic-terrain".equalsIgnoreCase(nameEn)
                    || "psychic terrain".equalsIgnoreCase(nameEn)) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            String nameEn = String.valueOf(move.get("name_en"));
            if ("reflect".equalsIgnoreCase(nameEn)
                    || "safeguard".equalsIgnoreCase(nameEn)
                    || "light-screen".equalsIgnoreCase(nameEn)
                    || "light screen".equalsIgnoreCase(nameEn)) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("taunt".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("disable".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            String nameEn = String.valueOf(move.get("name_en"));
            if ("heal-block".equalsIgnoreCase(nameEn) || "heal block".equalsIgnoreCase(nameEn)) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("torment".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("encore".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("yawn".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("spore".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("helping-hand".equalsIgnoreCase(String.valueOf(move.get("name_en"))) || "helping hand".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            String nameEn = String.valueOf(move.get("name_en"));
            if ("follow-me".equalsIgnoreCase(nameEn)
                    || "follow me".equalsIgnoreCase(nameEn)
                    || "rage-powder".equalsIgnoreCase(nameEn)
                    || "rage powder".equalsIgnoreCase(nameEn)) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("will-o-wisp".equalsIgnoreCase(String.valueOf(move.get("name_en"))) || "will o wisp".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("thunder-wave".equalsIgnoreCase(String.valueOf(move.get("name_en"))) || "thunder wave".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("icy-wind".equalsIgnoreCase(String.valueOf(move.get("name_en"))) || "icy wind".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        return utilities.get(0);
    }

    private Map<String, Object> normalizeMove(Map<String, Object> move) {
        Map<String, Object> normalized = new LinkedHashMap<>(move);
        normalized.put("power", toInt(move.get("power"), 0));
        normalized.put("accuracy", toInt(move.get("accuracy"), 100));
        normalized.put("priority", toInt(move.get("priority"), 0));
        normalized.put("damage_class_id", toInt(move.get("damage_class_id"), 0));
        normalized.put("type_id", toInt(move.get("type_id"), 0));
        normalized.put("target_id", toInt(move.get("target_id"), 0));
        normalized.put("min_hits", toInt(move.get("min_hits"), 0));
        normalized.put("max_hits", toInt(move.get("max_hits"), 0));
        normalized.put("crit_rate", toInt(move.get("crit_rate"), 0));
        normalized.put("effect_chance", toInt(move.get("effect_chance"), 0));
        normalized.put("ailment_chance", toInt(move.get("ailment_chance"), 0));
        normalized.put("flinch_chance", toInt(move.get("flinch_chance"), 0));
        normalized.put("stat_chance", toInt(move.get("stat_chance"), 0));
        normalized.put("drain", toInt(move.get("drain"), 0));
        normalized.put("healing", toInt(move.get("healing"), 0));
        normalized.put("ailment_name_en", String.valueOf(move.getOrDefault("ailment_name_en", "")));
        normalized.put("category_name_en", String.valueOf(move.getOrDefault("category_name_en", "")));
        normalized.put("effect_short", String.valueOf(move.getOrDefault("effect_short", "")));
        normalized.put("stat_changes", String.valueOf(move.getOrDefault("stat_changes", "")));
        normalized.put("flags", String.valueOf(move.getOrDefault("flags", "")));
        return normalized;
    }

    private int totalPower(List<Map<String, Object>> moves) {
        int total = 0;
        for (Map<String, Object> move : moves) {
            total += toInt(move.get("power"), 0);
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private void assignSpecialSystemProfile(Map<String, Object> pokemon, Random random) {
        int roll = random.nextInt(100);
        if (roll < 18) {
            Map<String, Object> stats = (Map<String, Object>) pokemon.get("stats");
            pokemon.put("megaEligible", true);
            pokemon.put("megaStats", Map.of(
                    "hp", stats.get("hp"),
                    "attack", (int) Math.floor(toInt(stats.get("attack"), 80) * 1.18d),
                    "defense", (int) Math.floor(toInt(stats.get("defense"), 80) * 1.12d),
                    "specialAttack", (int) Math.floor(toInt(stats.get("specialAttack"), 80) * 1.18d),
                    "specialDefense", (int) Math.floor(toInt(stats.get("specialDefense"), 80) * 1.12d),
                    "speed", (int) Math.floor(toInt(stats.get("speed"), 80) * 1.1d)
            ));
            return;
        }
        if (roll < 34) {
            pokemon.put("zMoveEligible", true);
            pokemon.put("heldItem", "normalium-z");
            return;
        }
        if (roll < 50) {
            pokemon.put("dynamaxEligible", true);
        }
    }

    private String pickUniqueItem(List<String> itemPool, Random random, List<Map<String, Object>> selectedMoves) {
        List<String> availableItems = new ArrayList<>(itemPool);
        if (selectedMoves.stream().anyMatch(this::isStatusMove)) {
            availableItems.remove("assault-vest");
        }
        if (availableItems.isEmpty()) {
            return "leftovers";
        }
        String chosen = availableItems.get(random.nextInt(availableItems.size()));
        itemPool.remove(chosen);
        return chosen;
    }

    private boolean isStatusMove(Map<String, Object> move) {
        return toInt(move.get("damage_class_id"), 0) == 3 || toInt(move.get("power"), 0) == 0;
    }

    private Set<String> normalizeNames(Set<String> names) {
        Set<String> normalized = new LinkedHashSet<>();
        if (names == null) {
            return normalized;
        }
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                normalized.add(name.toLowerCase());
            }
        }
        return normalized;
    }

    private int calculateHp(int base, int iv, int ev, int level) {
        return ((2 * base + iv + (ev / 4)) * level / 100) + level + 10;
    }

    private int calculateOtherStat(int base, int iv, int ev, int level, double natureMultiplier) {
        int raw = ((2 * base + iv + (ev / 4)) * level / 100) + 5;
        return (int) Math.floor(raw * natureMultiplier);
    }

    private Integer toInt(Object value, Integer fallback) {
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
