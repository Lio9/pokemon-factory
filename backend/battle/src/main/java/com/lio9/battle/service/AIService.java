package com.lio9.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.config.BattleConfig;
import com.lio9.battle.engine.MoveRegistry;
import com.lio9.battle.engine.TeamBalanceEvaluator;
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
    private final BattleConfig config;
    private final TeamBalanceEvaluator balanceEvaluator;
    private final BattleDexMapper battleDexMapper;
    private final ObjectMapper mapper;

    /**
     * 注入 battleFactory 侧使用的图鉴查询能力。
     */
    public AIService(BattleConfig config, TeamBalanceEvaluator balanceEvaluator,
            BattleDexMapper battleDexMapper, ObjectMapper mapper) {
        this.config = config;
        this.balanceEvaluator = balanceEvaluator;
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
     * 使用强度评估系统确保生成的队伍更加平衡
     */
    public String generateFactoryTeamJson(int size, int rank, long seed, Set<String> excludedNames) {
        int normalizedSize = Math.max(4, Math.min(6, size));
        int level = config.getLevel();

        // D6: 用确定性随机（seed）控制候选池打乱顺序，确保同 seed 生成相同队伍
        // SQL ORDER BY RANDOM() 不可控，改为先取大池再用 seeded Random 打乱
        List<Map<String, Object>> allCandidates = battleDexMapper.selectRandomDefaultForms(normalizedSize * 30);
        Random random = new Random(seed);
        java.util.Collections.shuffle(allCandidates, random);
        List<Map<String, Object>> candidates = allCandidates.subList(0, Math.min(allCandidates.size(), normalizedSize * 15));
        Set<String> excluded = normalizeNames(excludedNames);

        // 生成多个候选队伍，选择最平衡的
        List<List<Map<String, Object>>> candidateTeams = new ArrayList<>();

        for (int attempt = 0; attempt < 5; attempt++) {
            Set<String> usedSpecies = new LinkedHashSet<>();
            List<Map<String, Object>> roster = new ArrayList<>();
            List<String> itemPool = createItemPool();

            // 打乱候选顺序，增加多样性
            List<Map<String, Object>> shuffledCandidates = new ArrayList<>(candidates);
            Random shuffleRandom = new Random(seed + attempt);
            java.util.Collections.shuffle(shuffledCandidates, shuffleRandom);

            for (Map<String, Object> candidate : shuffledCandidates) {
                if (roster.size() >= normalizedSize) {
                    break;
                }

                String speciesName = String
                        .valueOf(candidate.getOrDefault("name_en", candidate.getOrDefault("name", "unknown")))
                        .toLowerCase();
                if (excluded.contains(speciesName) || usedSpecies.contains(speciesName)) {
                    continue;
                }

                Map<String, Object> pokemon = buildCompetitivePokemon(candidate, random, itemPool, level, rank);
                if (pokemon != null) {
                    roster.add(pokemon);
                    usedSpecies.add(speciesName);
                }
            }

            if (roster.size() == normalizedSize) {
                candidateTeams.add(roster);
            }
        }

        // 如果没有生成足够的队伍，返回第一个
        if (candidateTeams.isEmpty()) {
            return "[]";
        }

        // 评估所有候选队伍，选择最平衡的
        List<Map<String, Object>> bestTeam = candidateTeams.stream()
                .max(Comparator.comparingDouble(team -> balanceEvaluator.evaluateTeamBalance(team).getOverallScore()))
                .orElse(candidateTeams.get(0));

        try {
            return mapper.writeValueAsString(bestTeam);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 创建道具池
     */
    private List<String> createItemPool() {
        return new ArrayList<>(List.of(
                "focus-sash", "sitrus-berry", "assault-vest", "life-orb",
                "choice-scarf", "choice-band", "choice-specs", "clear-amulet",
                "covert-cloak", "mental-herb", "safety-goggles", "leftovers",
                "rocky-helmet", "light-clay", "terrain-extender",
                "mystic-water", "charcoal", "miracle-seed", "expert-belt",
                "muscle-band", "wise-glasses", "weakness-policy"));
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
    private Map<String, Object> buildCompetitivePokemon(Map<String, Object> candidate, Random random,
            List<String> itemPool, int level, int rank) {
        Integer formId = toInt(candidate.get("form_id"), null);
        if (formId == null) {
            return null;
        }

        List<Map<String, Object>> stats = battleDexMapper.selectFormStats(formId);
        List<Map<String, Object>> types = battleDexMapper.selectFormTypes(formId);
        List<Map<String, Object>> abilities = battleDexMapper.selectFormAbilities(formId);
        List<Map<String, Object>> movePool = battleDexMapper.selectCompetitiveMoves(formId, 60);
        if (stats == null || stats.isEmpty() || types == null || types.isEmpty() || movePool == null
                || movePool.isEmpty()) {
            return null;
        }
        if (abilities == null) {
            abilities = new ArrayList<>();
        }

        Map<Integer, Integer> statMap = new LinkedHashMap<>();
        for (Map<String, Object> stat : stats) {
            statMap.put(toInt(stat.get("stat_id"), 0), toInt(stat.get("base_stat"), 50));
        }

        // 智能判断定位
        String build = determineBuild(statMap);

        // 段位过滤：低段位排除过强宝可梦，高段位排除过弱宝可梦
        int bst = statMap.values().stream().mapToInt(Integer::intValue).sum();
        int minBst = 400 + rank * 30;  // 0→400, 1→430, 2→460, 3→490
        int maxBst = 520 + rank * 30;  // 0→520, 1→550, 2→580, 3→610
        if (bst < minBst || bst > maxBst) {
            return null; // 不符合段位强度范围，跳过
        }

        List<Map<String, Object>> selectedMoves = pickMoves(movePool, build, random, statMap.getOrDefault(6, 80), types);
        if (selectedMoves.size() < 4) {
            return null;
        }

        Map<String, Object> pokemon = new LinkedHashMap<>(candidate);
        pokemon.put("level", level);
        pokemon.put("types", types);
        pokemon.put("moves", selectedMoves);

        // 智能选择特性
        pokemon.put("ability", selectBestAbility(abilities, selectedMoves));

        // 智能选择道具（D6: 使用 seeded Random 而非 ThreadLocalRandom）
        String heldItem = selectBestItem(itemPool, pokemon, selectedMoves, random);
        pokemon.put("heldItem", heldItem == null ? "" : heldItem);

        Map<String, Object> teraType = pickTeraType(types, selectedMoves, random);
        pokemon.put("teraType", teraType);
        pokemon.put("teraTypeId", toInt(teraType.get("type_id"), 0));

        // 根据定位分配性格
        String nature = determineNature(build, statMap);
        pokemon.put("nature", nature);

        // 智能分配EV
        Map<String, Integer> evSpread = allocateEVs(statMap, build, selectedMoves);
        pokemon.put("evSpread", evSpread);

        // 构建战斗属性
        pokemon.put("stats", buildBattleStats(statMap, evSpread, nature));

        // 特殊系统分配（Mega/Z/极巨化）— 必须在 stats 之后，因为需要读取 stats
        assignSpecialSystemProfile(pokemon, random);

        // 更新道具（Z 招式可能覆盖为 normalium-z）
        heldItem = String.valueOf(pokemon.getOrDefault("heldItem", heldItem));
        pokemon.put("heldItem", heldItem == null ? "" : heldItem);
        // 道具展示信息（仅供前端预览，引擎仍读 heldItem 字符串）
        if (heldItem != null && !heldItem.isBlank()) {
            Map<String, Object> itemInfo = battleDexMapper.selectItemInfo(heldItem);
            if (itemInfo != null && !itemInfo.isEmpty()) {
                pokemon.put("heldItemInfo", itemInfo);
            }
        }

        // 计算战斗力评分
        double strength = balanceEvaluator.evaluatePokemonStrength(pokemon);
        pokemon.put("battleScore", (int) strength);

        // 可玩性校验：确保至少有 1 个攻击招式且属性不严重冲突
        if (!validatePlayability(pokemon, selectedMoves)) {
            return null;
        }

        return pokemon;
    }

    /**
     * 验证宝可梦的可玩性：确保至少有 1 个攻击招式且属性不严重冲突
     */
    private boolean validatePlayability(Map<String, Object> pokemon, List<Map<String, Object>> moves) {
        // 1. 检查是否有攻击招式
        boolean hasAttackMove = moves.stream()
                .anyMatch(move -> toInt(move.get("power"), 0) > 0);

        if (!hasAttackMove) {
            return false;
        }

        // 2. 检查招式多样性（避免全是同一种属性的招式）
        long distinctTypes = moves.stream()
                .map(m -> String.valueOf(m.get("type_id")))
                .distinct()
                .count();
        if (moves.size() >= 4 && distinctTypes < 2) {
            return false; // 4 个招式如果全是同一种属性，可玩性太低
        }

        // 3. 移除原先错误的"生命宝珠+火/电属性相冲"判定：
        //    Life Orb（生命宝珠）对任何属性都适用且无属性冲突，
        //    原逻辑混淆了 Life Orb 与 Mystic Water（神秘水滴），导致火/电系高攻宝可梦被误拒建队。
        return true;
    }

    /**
     * 智能判断定位（D2：新增坦克/辅助构建）
     * 根据种族值分布决定构建风格：
     * - physical/special/mixed：攻击向（原有逻辑）
     * - tank：HP+双防种族值高，攻击较低 → 耐久向
     * - support：速度慢且有辅助招式池 → 辅助向
     */
    private String determineBuild(Map<Integer, Integer> statMap) {
        int atk = statMap.getOrDefault(2, 50);
        int spa = statMap.getOrDefault(4, 50);
        int hp = statMap.getOrDefault(1, 80);
        int def = statMap.getOrDefault(3, 60);
        int spd = statMap.getOrDefault(5, 60);
        int spe = statMap.getOrDefault(6, 80);

        int offensivePower = Math.max(atk, spa);
        int bulk = hp + def + spd;

        // 坦克判定：耐久种族值总和 > 250 且攻击不高
        if (bulk >= 255 && offensivePower < 100) {
            return "tank";
        }
        // 辅助判定：速度慢（<70）且攻击不高
        if (spe < 70 && offensivePower < 90 && bulk >= 220) {
            return "support";
        }
        if (atk >= spa * 1.15)
            return "physical";
        if (spa >= atk * 1.15)
            return "special";
        return "mixed";
    }

    /**
     * 确定性格（D2：坦克/辅助使用防御性格）
     */
    private String determineNature(String build, Map<Integer, Integer> statMap) {
        int spe = statMap.getOrDefault(6, 80);

        switch (build) {
            case "physical":
                return spe >= 90 ? "jolly" : "adamant";
            case "special":
                return spe >= 90 ? "timid" : "modest";
            case "tank":
                // 坦克：根据物防/特防哪个更低来决定加哪边
                int def = statMap.getOrDefault(3, 60);
                int spd = statMap.getOrDefault(5, 60);
                return def <= spd ? "bold" : "calm";
            case "support":
                return "calm"; // 辅助偏特防（多数辅助招是特攻端威胁）
            default:
                return "serious";
        }
    }

    /**
     * 智能分配EV
     */
    private Map<String, Integer> allocateEVs(Map<Integer, Integer> baseStats, String build,
            List<Map<String, Object>> moves) {
        int spe = baseStats.getOrDefault(6, 80);

        boolean hasSetupMove = moves.stream().anyMatch(this::isSetupMove);

        Map<String, Integer> evs = new LinkedHashMap<>();

        if ("physical".equals(build)) {
            // 物理攻击手
            if (spe >= 100) {
                // 高速：全攻速
                evs.put("hp", 4);
                evs.put("atk", 252);
                evs.put("def", 0);
                evs.put("spa", 0);
                evs.put("spd", 0);
                evs.put("spe", 252);
            } else if (hasSetupMove) {
                // 有强化：适当耐久
                evs.put("hp", 252);
                evs.put("atk", 252);
                evs.put("def", 0);
                evs.put("spa", 0);
                evs.put("spd", 4);
                evs.put("spe", 0);
            } else {
                // 标准分配
                evs.put("hp", 4);
                evs.put("atk", 252);
                evs.put("def", 0);
                evs.put("spa", 0);
                evs.put("spd", 0);
                evs.put("spe", 252);
            }
        } else if ("special".equals(build)) {
            // 特殊攻击手
            if (spe >= 100) {
                evs.put("hp", 4);
                evs.put("atk", 0);
                evs.put("def", 0);
                evs.put("spa", 252);
                evs.put("spd", 0);
                evs.put("spe", 252);
            } else if (hasSetupMove) {
                evs.put("hp", 252);
                evs.put("atk", 0);
                evs.put("def", 0);
                evs.put("spa", 252);
                evs.put("spd", 4);
                evs.put("spe", 0);
            } else {
                evs.put("hp", 4);
                evs.put("atk", 0);
                evs.put("def", 0);
                evs.put("spa", 252);
                evs.put("spd", 0);
                evs.put("spe", 252);
            }
        } else if ("tank".equals(build)) {
            // 坦克：全HP + 物防或特防（取较低方补强）
            int def = baseStats.getOrDefault(3, 60);
            int spd = baseStats.getOrDefault(5, 60);
            evs.put("hp", 252);
            evs.put("atk", 0);
            evs.put("def", def <= spd ? 252 : 0);
            evs.put("spa", 0);
            evs.put("spd", def > spd ? 252 : 4);
            evs.put("spe", def <= spd ? 4 : 0);
        } else if ("support".equals(build)) {
            // 辅助：全HP + 双防分散，不需要速度
            evs.put("hp", 252);
            evs.put("atk", 0);
            evs.put("def", 128);
            evs.put("spa", 0);
            evs.put("spd", 128);
            evs.put("spe", 0);
        } else {
            // 混合攻击手
            evs.put("hp", 4);
            evs.put("atk", 252);
            evs.put("def", 0);
            evs.put("spa", 252);
            evs.put("spd", 0);
            evs.put("spe", 0);
        }

        return evs;
    }

    /**
     * 智能选择特性
     */
    private Map<String, Object> selectBestAbility(List<Map<String, Object>> abilities,
            List<Map<String, Object>> moves) {
        if (abilities == null || abilities.isEmpty()) {
            return null;
        }
        if (abilities.size() == 1) {
            return abilities.get(0);
        }

        // S级特性列表
        Set<String> sTierAbilities = Set.of(
                "adaptability", "aerilate", "pixilate", "refrigerate", "galvanize",
                "dragons-maw", "transistor", "steely-spirit");

        // A级特性列表
        Set<String> aTierAbilities = Set.of(
                "technician", "sheer-force", "iron-fist", "reckless", "guts",
                "flare-boost", "toxic-boost", "tinted-lens", "analytic",
                "levitate", "thick-fat", "water-absorb", "volt-absorb",
                "flash-fire", "storm-drain", "sap-sipper");

        // 评分并选择最高分的特性
        Map<String, Object> bestAbility = null;
        double bestScore = -1;

        for (Map<String, Object> ability : abilities) {
            String nameEn = String.valueOf(ability.getOrDefault("name_en", "")).toLowerCase();
            double score = 5.0; // 基础分

            if (sTierAbilities.contains(nameEn)) {
                score = 25.0;
            } else if (aTierAbilities.contains(nameEn)) {
                score = 18.0;
            }

            // 检查与招式的协同
            if ("technician".equals(nameEn) && hasLowPowerMoves(moves)) {
                score += 10;
            }
            if ("sheer-force".equals(nameEn) && hasSecondaryEffectMoves(moves)) {
                score += 8;
            }

            if (score > bestScore) {
                bestScore = score;
                bestAbility = ability;
            }
        }

        return bestAbility != null ? bestAbility : abilities.get(0);
    }

    /**
     * 智能选择道具（更多样化、更合理）
     */
    @SuppressWarnings("unchecked")
    private String selectBestItem(List<String> itemPool, Map<String, Object> pokemon,
            List<Map<String, Object>> moves, Random random) {
        if (itemPool == null || itemPool.isEmpty()) {
            return null;
        }

        Map<String, Object> stats = (Map<String, Object>) pokemon.get("stats");
        int spe = stats != null ? toInt(stats.get("speed"), 0) : 0;
        int atk = stats != null ? toInt(stats.get("attack"), 0) : 0;
        int spa = stats != null ? toInt(stats.get("specialAttack"), 0) : 0;
        int hp = stats != null ? toInt(stats.get("hp"), 0) : 0;
        int def = stats != null ? toInt(stats.get("defense"), 0) : 0;
        int spd = stats != null ? toInt(stats.get("specialDefense"), 0) : 0;
        boolean isPhysical = atk >= spa;
        boolean hasSetupMove = moves.stream().anyMatch(this::isSetupMove);
        boolean hasStatusMoves = moves.stream().anyMatch(m -> toInt(m.get("damage_class_id"), 0) == 3
                && !"protect".equalsIgnoreCase(String.valueOf(m.get("name_en"))));
        boolean isBulky = hp >= 150 || (def + spd) >= 190;

        // 策略化道具选择
        String selectedItem;

        if (hasSetupMove && !hasStatusMoves) {
            // 强化攻击手：生命宝珠
            selectedItem = "life-orb";
        } else if (spe >= 105 && !isBulky) {
            // 高速攻击手：讲究围巾/头带/眼镜
            selectedItem = isPhysical ? "choice-band" : "choice-specs";
        } else if (spe < 55 && isBulky) {
            // 低速坦克：突击背心（无变化招）或吃剩饭（有变化招）
            selectedItem = hasStatusMoves ? "leftovers" : "assault-vest";
        } else if (isBulky && hasStatusMoves) {
            // 耐久辅助：吃剩饭
            selectedItem = "leftovers";
        } else if (isBulky) {
            // 耐久攻击手：突击背心或吃剩饭
            selectedItem = random.nextBoolean() ? "assault-vest" : "leftovers";
        } else if (spe >= 80 && spe < 105) {
            // 中速攻击手：气势披带或生命宝珠
            selectedItem = random.nextBoolean() ? "focus-sash" : "life-orb";
        } else {
            // 其他：从道具池随机
            selectedItem = itemPool.get(random.nextInt(itemPool.size()));
        }

        // 从道具池中移除已选道具，避免重复
        itemPool.remove(selectedItem);

        return selectedItem;
    }

    private boolean isSetupMove(Map<String, Object> move) {
        return MoveRegistry.isSwordsDance(move) ||
                MoveRegistry.isNastyPlot(move) ||
                MoveRegistry.isDragonDance(move) ||
                MoveRegistry.isCalmMind(move) ||
                MoveRegistry.isAgility(move) ||
                MoveRegistry.isBulkUp(move) ||
                MoveRegistry.isQuiverDance(move);
    }

    private boolean hasLowPowerMoves(List<Map<String, Object>> moves) {
        return moves.stream()
                .anyMatch(m -> !MoveRegistry.isStatusMove(m) && toInt(m.get("power"), 0) <= 60);
    }

    private boolean hasSecondaryEffectMoves(List<Map<String, Object>> moves) {
        return moves.stream()
                .anyMatch(m -> toInt(m.get("effect_chance"), 0) > 0);
    }

    private Map<String, Object> buildBattleStats(Map<Integer, Integer> baseStats, Map<String, Integer> evSpread,
            String nature) {
        Map<String, Object> stats = new LinkedHashMap<>();

        int hpIV = 31, atkIV = 31, defIV = 31, spaIV = 31, spdIV = 31, speIV = 31;
        int hpEV = evSpread.getOrDefault("hp", 0);
        int atkEV = evSpread.getOrDefault("atk", 0);
        int defEV = evSpread.getOrDefault("def", 0);
        int spaEV = evSpread.getOrDefault("spa", 0);
        int spdEV = evSpread.getOrDefault("spd", 0);
        int speEV = evSpread.getOrDefault("spe", 0);

        // 性格修正
        double atkNatureMod = 1.0, spaNatureMod = 1.0, defNatureMod = 1.0, spdNatureMod = 1.0, speNatureMod = 1.0;

        switch (nature != null ? nature : "") {
            case "adamant":
                atkNatureMod = 1.1;
                spaNatureMod = 0.9;
                break;
            case "modest":
                spaNatureMod = 1.1;
                atkNatureMod = 0.9;
                break;
            case "jolly":
                speNatureMod = 1.1;
                spaNatureMod = 0.9;
                break;
            case "timid":
                speNatureMod = 1.1;
                atkNatureMod = 0.9;
                break;
            case "bold":
                defNatureMod = 1.1;
                atkNatureMod = 0.9;
                break;
            case "calm":
                spdNatureMod = 1.1;
                atkNatureMod = 0.9;
                break;
            default:
                break;
        }

        int level = config.getLevel();
        stats.put("hp", calculateHp(baseStats.getOrDefault(1, 80), hpIV, hpEV, level));
        stats.put("attack", calculateOtherStat(baseStats.getOrDefault(2, 80), atkIV, atkEV, level, atkNatureMod));
        stats.put("defense", calculateOtherStat(baseStats.getOrDefault(3, 80), defIV, defEV, level, defNatureMod));
        stats.put("specialAttack",
                calculateOtherStat(baseStats.getOrDefault(4, 80), spaIV, spaEV, level, spaNatureMod));
        stats.put("specialDefense",
                calculateOtherStat(baseStats.getOrDefault(5, 80), spdIV, spdEV, level, spdNatureMod));
        stats.put("speed", calculateOtherStat(baseStats.getOrDefault(6, 80), speIV, speEV, level, speNatureMod));

        return stats;
    }

    /**
     * 智能选招（重写）：
     * 1. 保证至少 1-2 个 STAB 招式（本系高威力）
     * 2. 补充覆盖招式（不同属性的攻击）
     * 3. 加一个实用辅助招式（保护/顺风/控速等）
     * 4. 不足 4 个时用次优攻击补满
     */
    private List<Map<String, Object>> pickMoves(List<Map<String, Object>> movePool, String build, Random random,
            int baseSpeed, List<Map<String, Object>> pokemonTypes) {
        // 收集本系 type_id
        Set<Integer> stabTypeIds = new LinkedHashSet<>();
        if (pokemonTypes != null) {
            for (Map<String, Object> t : pokemonTypes) {
                stabTypeIds.add(toInt(t.get("type_id"), 0));
            }
        }
        // 分类招式
        List<Map<String, Object>> attacks = new ArrayList<>();
        Map<String, Object> protect = null;
        List<Map<String, Object>> utilities = new ArrayList<>();
        List<Map<String, Object>> setupMoves = new ArrayList<>();

        for (Map<String, Object> move : movePool) {
            String nameEn = String.valueOf(move.getOrDefault("name_en", "")).toLowerCase().replace(" ", "-");
            int damageClassId = toInt(move.get("damage_class_id"), 0);
            int power = toInt(move.get("power"), 0);

            // 保护
            if ("protect".equalsIgnoreCase(nameEn) || "detect".equalsIgnoreCase(nameEn)) {
                protect = normalizeMove(move);
                continue;
            }

            // 强化招式
            if (isSetupMove(move)) {
                setupMoves.add(normalizeMove(move));
                continue;
            }

            // 辅助/控速/场地/状态招式
            if (isUtilityMove(nameEn)) {
                utilities.add(normalizeMove(move));
                continue;
            }

            // 攻击招式（按 build 过滤）
            if (power > 0) {
                if ("physical".equals(build) && damageClassId == 1) attacks.add(normalizeMove(move));
                else if ("special".equals(build) && damageClassId == 2) attacks.add(normalizeMove(move));
                else if ("mixed".equals(build) && damageClassId <= 2) attacks.add(normalizeMove(move));
                else if (("tank".equals(build) || "support".equals(build)) && damageClassId <= 2) attacks.add(normalizeMove(move));
            }
        }

        // 按威力降序排列
        attacks.sort(Comparator.comparingInt((Map<String, Object> m) -> toInt(m.get("power"), 0)).reversed()
                .thenComparingInt(m -> toInt(m.get("accuracy"), 100)).reversed());

        List<Map<String, Object>> selected = new ArrayList<>();
        Set<Integer> usedTypes = new LinkedHashSet<>();

        // ① 优先选 STAB 招式（本系攻击，威力 ≥ 50）
        for (Map<String, Object> move : attacks) {
            if (selected.size() >= 2) break;
            int typeId = toInt(move.get("type_id"), 0);
            int power = toInt(move.get("power"), 0);
            if (power >= 50 && stabTypeIds.contains(typeId) && usedTypes.add(typeId)) {
                selected.add(move);
            }
        }

        // ② 补充覆盖招式（不同属性，威力 ≥ 50）
        for (Map<String, Object> move : attacks) {
            if (selected.size() >= 3) break;
            int typeId = toInt(move.get("type_id"), 0);
            int power = toInt(move.get("power"), 0);
            if (power >= 50 && usedTypes.add(typeId)) {
                selected.add(move);
            }
        }

        // ③ 加一个强化招式（如果有）
        if (!setupMoves.isEmpty() && selected.size() < 4) {
            selected.add(setupMoves.get(random.nextInt(setupMoves.size())));
        }

        // ④ 加一个辅助招式（如果有空间）
        if (selected.size() < 4) {
            Map<String, Object> utility = preferredUtility(utilities, baseSpeed);
            if (utility != null) {
                selected.add(utility);
            }
        }

        // ⑤ 加保护（如果有空间）
        if (protect != null && selected.size() < 4) {
            selected.add(protect);
        }

        // ⑥ 不足 4 个 → 用剩余攻击补满
        for (Map<String, Object> move : attacks) {
            if (selected.size() >= 4) break;
            if (selected.stream().noneMatch(s -> s.get("name_en").equals(move.get("name_en")))) {
                selected.add(move);
            }
        }

        // ⑦ 还不够 → 随机补
        List<Map<String, Object>> remaining = new ArrayList<>(attacks);
        remaining.removeIf(m -> selected.stream().anyMatch(s -> s.get("name_en").equals(m.get("name_en"))));
        while (selected.size() < 4 && !remaining.isEmpty()) {
            selected.add(remaining.remove(random.nextInt(remaining.size())));
        }

        // 确保至少有 1 个招式
        if (selected.isEmpty() && !attacks.isEmpty()) {
            selected.add(attacks.get(0));
        }

        return selected;
    }

    /** 判断是否为辅助/控速/场地类招式 */
    private boolean isUtilityMove(String nameEn) {
        return switch (nameEn) {
            case "tailwind", "trick-room", "wide-guard", "quick-guard",
                 "rain-dance", "sunny-day", "aurora-veil",
                 "electric-terrain", "psychic-terrain", "grassy-terrain", "misty-terrain",
                 "reflect", "safeguard", "light-screen",
                 "taunt", "disable", "encore", "yawn", "spore",
                 "helping-hand", "ally-switch", "fake-tears", "parting-shot",
                 "follow-me", "rage-powder", "will-o-wisp", "thunder-wave", "icy-wind",
                 "heal-bell", "aromatherapy", "trick", "knock-off",
                 "stealth-rock", "spikes", "toxic-spikes", "sticky-web" -> true;
            default -> false;
        };
    }

    private Map<String, Object> pickTeraType(List<Map<String, Object>> types, List<Map<String, Object>> moves,
            Random random) {
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
                if ("trick-room".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                        || "trick room".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
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
            if ("helping-hand".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                    || "helping hand".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
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
            if ("will-o-wisp".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                    || "will o wisp".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("thunder-wave".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                    || "thunder wave".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        for (Map<String, Object> move : utilities) {
            if ("icy-wind".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                    || "icy wind".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
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
        // 保留 PP 字段（pp 可能为 null，交由引擎侧 normalizeMoves 兜底）
        normalized.put("pp", move.get("pp"));
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
                    "speed", (int) Math.floor(toInt(stats.get("speed"), 80) * 1.1d)));
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
