package com.lio9.battle.engine;

import com.lio9.common.util.BattleUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 队伍平衡性评估器
 * 用于评估宝可梦和队伍的战斗力，确保生成的队伍更加平衡
 */
@Component
public class TeamBalanceEvaluator {

    /**
     * 评估单只宝可梦的战斗力分数
     *
     * @param pokemon 宝可梦数据
     * @return 战斗力分数 (越高越强)
     */
    /**
     * D3: 评估单只宝可梦的战斗力（归一化到 0-100 区间，解决饱和失真）。
     *
     * <p>旧公式各项直接累加导致所有宝可梦得分集中在 400-600，
     * 无法区分强弱层级。新公式将每个维度归一化后加权合并。</p>
     */
    public double evaluatePokemonStrength(Map<String, Object> pokemon) {
        if (pokemon == null)
            return 0.0;

        // 1. 种族值总和 (BST) 归一化：最高 ~720(传说) → 100, 最低 ~200 → 0
        Map<String, Object> stats = BattleUtils.toMap(pokemon.get("stats"));
        int bst = calculateBST(stats);
        double bstScore = Math.max(0, Math.min(100, (bst - 200.0) / 5.2)); // 200→0, 720→100

        // 2. 招式质量 (0-100)
        List<Map<String, Object>> moves = castList(pokemon.get("moves"));
        double moveScore = 30.0; // 基线：有4个招式
        if (!moves.isEmpty()) {
            double avgPower = calculateAverageMovePower(moves);
            moveScore = Math.min(60, avgPower * 0.5); // 威力0-120 → 0-60
            Set<Integer> coveredTypes = getCoveredTypes(moves);
            moveScore += Math.min(15, coveredTypes.size() * 4); // 属性覆盖 +15 max
            long setupMoves = moves.stream().filter(this::isSetupMove).count();
            moveScore += Math.min(10, setupMoves * 10);
            long recoveryMoves = moves.stream().filter(this::isRecoveryMove).count();
            moveScore += Math.min(8, recoveryMoves * 8);
            long priorityMoves = moves.stream().filter(this::isPriorityMove).count();
            moveScore += Math.min(7, priorityMoves * 7);
        }
        moveScore = Math.min(100, moveScore);

        // 3. 特性质量 (0-100)
        String ability = BattleUtils.toString(pokemon.get("ability"), "");
        double abilityScore = Math.min(100, evaluateAbilityQuality(ability, moves));

        // 4. 道具质量 (0-100)
        String item = BattleUtils.toString(pokemon.get("heldItem"), "");
        double itemScore = Math.min(100, evaluateItemQuality(item, pokemon, moves));

        // 5. 速度 (0-100)
        int speed = BattleUtils.toInt(stats.get("speed"), 0);
        double speedScore = Math.min(100, speed * 0.65); // 154→100

        // 加权合并：BST 40% + 招式 25% + 特性 15% + 道具 10% + 速度 10%
        return bstScore * 0.40 + moveScore * 0.25 + abilityScore * 0.15
                + itemScore * 0.10 + speedScore * 0.10;
    }

    /**
     * 评估整个队伍的平衡性
     */
    public TeamBalanceScore evaluateTeamBalance(List<Map<String, Object>> team) {
        TeamBalanceScore score = new TeamBalanceScore();

        if (team == null || team.isEmpty()) {
            return score;
        }

        // 1. 总战斗力
        double totalStrength = team.stream()
                .mapToDouble(this::evaluatePokemonStrength)
                .sum();
        score.setTotalStrength(totalStrength);
        score.setAvgStrength(totalStrength / team.size());

        // 2. 属性多样性
        Set<Integer> allTypes = new HashSet<>();
        for (Map<String, Object> mon : team) {
            allTypes.addAll(getPokemonTypes(mon));
        }
        score.setTypeCoverage(allTypes.size());
        score.setTypeDiversityRatio((double) allTypes.size() / 18.0); // 18种属性

        // 3. 角色分布分析
        Map<String, Integer> roles = analyzeTeamRoles(team);
        score.setRoleDistribution(roles);

        // 4. 速度层次
        List<Integer> speeds = team.stream()
                .map(m -> BattleUtils.toInt(BattleUtils.toMap(m.get("stats")).get("speed"), 0))
                .sorted()
                .collect(Collectors.toList());
        score.setSpeedLayers(speeds);

        // 计算速度多样性 (标准差)
        if (!speeds.isEmpty()) {
            double avgSpeed = speeds.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);
            double variance = speeds.stream()
                    .mapToDouble(s -> Math.pow(s.doubleValue() - avgSpeed, 2))
                    .average()
                    .orElse(0);
            score.setSpeedVariance(Math.sqrt(variance));
        }

        // 5. 特殊系统分布
        long megaCount = team.stream().filter(m -> Boolean.TRUE.equals(m.get("megaEligible"))).count();
        long zMoveCount = team.stream().filter(m -> Boolean.TRUE.equals(m.get("zMoveEligible"))).count();
        long dynamaxCount = team.stream().filter(m -> Boolean.TRUE.equals(m.get("dynamaxEligible"))).count();
        score.setMegaCount((int) megaCount);
        score.setZMoveCount((int) zMoveCount);
        score.setDynamaxCount((int) dynamaxCount);

        // 6. 综合评分 (0-100)
        score.setOverallScore(calculateOverallScore(score, team.size()));

        return score;
    }

    /**
     * 判断两个队伍的强度是否匹配
     *
     * @param team1     队伍1
     * @param team2     队伍2
     * @param tolerance 容差 (0.0-1.0)，越小要求越严格
     * @return 是否匹配
     */
    public boolean isTeamMatched(List<Map<String, Object>> team1, List<Map<String, Object>> team2, double tolerance) {
        double strength1 = evaluateTeamBalance(team1).getTotalStrength();
        double strength2 = evaluateTeamBalance(team2).getTotalStrength();

        double maxStrength = Math.max(strength1, strength2);
        double minStrength = Math.min(strength1, strength2);

        if (maxStrength == 0)
            return true;

        double ratio = minStrength / maxStrength;
        return ratio >= (1.0 - tolerance);
    }

    // ==================== 私有方法 ====================

    private int calculateBST(Map<String, Object> stats) {
        if (stats == null || stats.isEmpty())
            return 0;
        return BattleUtils.toInt(stats.get("hp"), 0) +
                BattleUtils.toInt(stats.get("attack"), 0) +
                BattleUtils.toInt(stats.get("defense"), 0) +
                BattleUtils.toInt(stats.get("specialAttack"), 0) +
                BattleUtils.toInt(stats.get("specialDefense"), 0) +
                BattleUtils.toInt(stats.get("speed"), 0);
    }

    private double calculateAverageMovePower(List<Map<String, Object>> moves) {
        if (moves.isEmpty())
            return 0.0;

        long damagingMoves = moves.stream()
                .filter(m -> !MoveRegistry.isStatusMove(m))
                .count();

        if (damagingMoves == 0)
            return 0.0;

        double totalPower = moves.stream()
                .filter(m -> !MoveRegistry.isStatusMove(m))
                .mapToDouble(m -> BattleUtils.toInt(m.get("power"), 0))
                .sum();

        return totalPower / damagingMoves;
    }

    private Set<Integer> getCoveredTypes(List<Map<String, Object>> moves) {
        return moves.stream()
                .filter(m -> !MoveRegistry.isStatusMove(m))
                .map(m -> BattleUtils.toInt(m.get("type_id"), 0))
                .filter(typeId -> typeId > 0)
                .collect(Collectors.toSet());
    }

    private Set<Integer> getPokemonTypes(Map<String, Object> pokemon) {
        List<Map<String, Object>> types = castList(pokemon.get("types"));
        return types.stream()
                .map(t -> BattleUtils.toInt(t.get("type_id"), 0))
                .filter(typeId -> typeId > 0)
                .collect(Collectors.toSet());
    }

    private boolean isSetupMove(Map<String, Object> move) {
        return MoveRegistry.isSwordsDance(move) ||
                MoveRegistry.isNastyPlot(move) ||
                MoveRegistry.isDragonDance(move) ||
                MoveRegistry.isCalmMind(move) ||
                MoveRegistry.isAgility(move) ||
                MoveRegistry.isBulkUp(move) ||
                MoveRegistry.isQuiverDance(move) ||
                MoveRegistry.isCoil(move) ||
                MoveRegistry.isShellSmash(move);
    }

    private boolean isRecoveryMove(Map<String, Object> move) {
        return MoveRegistry.isRecover(move) ||
                MoveRegistry.isRoost(move) ||
                MoveRegistry.isRest(move) ||
                MoveRegistry.isSoftBoiled(move) ||
                MoveRegistry.isSynthesis(move) ||
                MoveRegistry.isMoonlight(move);
    }

    private boolean isPriorityMove(Map<String, Object> move) {
        return BattleUtils.toInt(move.get("priority"), 0) > 0;
    }

    private double evaluateAbilityQuality(String ability, List<Map<String, Object>> moves) {
        if (ability == null || ability.isBlank())
            return 0.0;

        // S级特性
        Set<String> sTierAbilities = Set.of(
                "adaptability", "aerilate", "pixilate", "refrigerate", "galvanize",
                "dragons-maw", "transistor", "steely-spirit");

        // A级特性
        Set<String> aTierAbilities = Set.of(
                "technician", "sheer-force", "iron-fist", "reckless", "guts",
                "flare-boost", "toxic-boost", "tinted-lens", "analytic",
                "levitate", "thick-fat", "water-absorb", "volt-absorb",
                "flash-fire", "storm-drain", "sap-sipper");

        // B级特性
        Set<String> bTierAbilities = Set.of(
                "hustle", "sand-force", "strong-jaw", "mega-launcher",
                "punk-rock", "normalize", "super-luck", "sniper");

        String lowerAbility = ability.toLowerCase();

        if (sTierAbilities.contains(lowerAbility))
            return 25.0;
        if (aTierAbilities.contains(lowerAbility))
            return 18.0;
        if (bTierAbilities.contains(lowerAbility))
            return 12.0;

        return 5.0; // 普通特性
    }

    private double evaluateItemQuality(String item, Map<String, Object> pokemon, List<Map<String, Object>> moves) {
        if (item == null || item.isBlank())
            return 0.0;

        // S级道具
        Set<String> sTierItems = Set.of(
                "life-orb", "choice-band", "choice-specs", "choice-scarf");

        // A级道具
        Set<String> aTierItems = Set.of(
                "assault-vest", "focus-sash", "leftovers", "expert-belt",
                "weakness-policy", "muscle-band", "wise-glasses");

        // B级道具
        Set<String> bTierItems = Set.of(
                "sitrus-berry", "lum-berry", "air-balloon", "heavy-duty-boots");

        String lowerItem = item.toLowerCase().replace(" ", "-");

        if (sTierItems.contains(lowerItem))
            return 20.0;
        if (aTierItems.contains(lowerItem))
            return 15.0;
        if (bTierItems.contains(lowerItem))
            return 10.0;

        return 5.0; // 其他道具
    }

    private Map<String, Integer> analyzeTeamRoles(List<Map<String, Object>> team) {
        Map<String, Integer> roles = new HashMap<>();
        roles.put("sweeper", 0);
        roles.put("tank", 0);
        roles.put("support", 0);
        roles.put("mixed", 0);

        for (Map<String, Object> mon : team) {
            String role = determinePokemonRole(mon);
            roles.merge(role, 1, (a, b) -> a + b);
        }

        return roles;
    }

    private String determinePokemonRole(Map<String, Object> pokemon) {
        Map<String, Object> stats = BattleUtils.toMap(pokemon.get("stats"));
        List<Map<String, Object>> moves = castList(pokemon.get("moves"));

        int atk = BattleUtils.toInt(stats.get("attack"), 0);
        int spa = BattleUtils.toInt(stats.get("specialAttack"), 0);
        int def = BattleUtils.toInt(stats.get("defense"), 0);
        int spd = BattleUtils.toInt(stats.get("specialDefense"), 0);
        int spe = BattleUtils.toInt(stats.get("speed"), 0);

        long physicalMoves = moves.stream().filter(m -> isPhysicalMove(m)).count();
        long specialMoves = moves.stream().filter(m -> isSpecialMove(m)).count();
        long statusMoves = moves.stream().filter(MoveRegistry::isStatusMove).count();

        // 辅助型
        if (statusMoves >= 3)
            return "support";

        // 坦克型
        if ((def + spd) > (atk + spa) && (def + spd) > 180) {
            return "tank";
        }

        // 攻击手
        if (physicalMoves > specialMoves && atk > spa) {
            return spe > 90 ? "sweeper" : "tank";
        }
        if (specialMoves > physicalMoves && spa > atk) {
            return spe > 90 ? "sweeper" : "tank";
        }

        return "mixed";
    }

    private boolean isPhysicalMove(Map<String, Object> move) {
        return BattleUtils.toInt(move.get("damage_class_id"), 0) == 1 &&
                !MoveRegistry.isStatusMove(move);
    }

    private boolean isSpecialMove(Map<String, Object> move) {
        return BattleUtils.toInt(move.get("damage_class_id"), 0) == 2 &&
                !MoveRegistry.isStatusMove(move);
    }

    private double calculateOverallScore(TeamBalanceScore score, int teamSize) {
        double overall = 0.0;

        // 平均强度 (40%权重)
        overall += Math.min(score.getAvgStrength() / 300.0, 1.0) * 40;

        // 属性多样性 (20%权重)
        overall += score.getTypeDiversityRatio() * 20;

        // 角色分布合理性 (20%权重)
        Map<String, Integer> roles = score.getRoleDistribution();
        int sweepers = roles.getOrDefault("sweeper", 0);
        int tanks = roles.getOrDefault("tank", 0);
        int supports = roles.getOrDefault("support", 0);

        // 理想分布: 2 sweeper + 2 tank + 0-1 support (对于4人队)
        double roleScore = 0.0;
        if (teamSize == 4) {
            if (sweepers >= 1 && sweepers <= 3)
                roleScore += 0.4;
            if (tanks >= 1 && tanks <= 3)
                roleScore += 0.4;
            if (supports <= 2)
                roleScore += 0.2;
        }
        overall += roleScore * 20;

        // 速度层次 (20%权重)
        double speedVariance = score.getSpeedVariance();
        overall += Math.min(speedVariance / 50.0, 1.0) * 20;

        return Math.min(overall, 100.0);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return Collections.emptyList();
    }

    /**
     * 队伍平衡性评分
     */
    public static class TeamBalanceScore {
        private double totalStrength;
        private double avgStrength;
        private int typeCoverage;
        private double typeDiversityRatio;
        private Map<String, Integer> roleDistribution;
        private List<Integer> speedLayers;
        private double speedVariance;
        private int megaCount;
        private int zMoveCount;
        private int dynamaxCount;
        private double overallScore;

        // Getters and Setters
        public double getTotalStrength() {
            return totalStrength;
        }

        public void setTotalStrength(double totalStrength) {
            this.totalStrength = totalStrength;
        }

        public double getAvgStrength() {
            return avgStrength;
        }

        public void setAvgStrength(double avgStrength) {
            this.avgStrength = avgStrength;
        }

        public int getTypeCoverage() {
            return typeCoverage;
        }

        public void setTypeCoverage(int typeCoverage) {
            this.typeCoverage = typeCoverage;
        }

        public double getTypeDiversityRatio() {
            return typeDiversityRatio;
        }

        public void setTypeDiversityRatio(double typeDiversityRatio) {
            this.typeDiversityRatio = typeDiversityRatio;
        }

        public Map<String, Integer> getRoleDistribution() {
            return roleDistribution;
        }

        public void setRoleDistribution(Map<String, Integer> roleDistribution) {
            this.roleDistribution = roleDistribution;
        }

        public List<Integer> getSpeedLayers() {
            return speedLayers;
        }

        public void setSpeedLayers(List<Integer> speedLayers) {
            this.speedLayers = speedLayers;
        }

        public double getSpeedVariance() {
            return speedVariance;
        }

        public void setSpeedVariance(double speedVariance) {
            this.speedVariance = speedVariance;
        }

        public int getMegaCount() {
            return megaCount;
        }

        public void setMegaCount(int megaCount) {
            this.megaCount = megaCount;
        }

        public int getZMoveCount() {
            return zMoveCount;
        }

        public void setZMoveCount(int zMoveCount) {
            this.zMoveCount = zMoveCount;
        }

        public int getDynamaxCount() {
            return dynamaxCount;
        }

        public void setDynamaxCount(int dynamaxCount) {
            this.dynamaxCount = dynamaxCount;
        }

        public double getOverallScore() {
            return overallScore;
        }

        public void setOverallScore(double overallScore) {
            this.overallScore = overallScore;
        }
    }
}
