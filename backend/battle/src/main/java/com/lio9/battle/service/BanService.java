package com.lio9.battle.service;

import com.lio9.battle.mapper.PlayerMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Ban 系统服务。
 * <p>
 * 规则：
 * - 每次工厂挑战开始前，玩家可以 ban 掉不想遇到的宝可梦
 * - Ban 的宝可梦不会出现在对手队伍中
 * - Ban 位置最多 6 只，费用梯度递增：
 *   第1只免费，第2只100分，第3只300分，第4只600分，第5只1000分，第6只1500分
 * </p>
 */
@Service
public class BanService {

    /** Ban 位置费用梯度（索引=第N只，值=费用） */
    private static final int[] BAN_COSTS = {0, 100, 300, 600, 1000, 1500};

    /** 最大 ban 数量 */
    public static final int MAX_BAN_SLOTS = 6;

    private final PlayerMapper playerMapper;

    public BanService(PlayerMapper playerMapper) {
        this.playerMapper = playerMapper;
    }

    /**
     * 获取 ban 位置费用信息
     */
    public Map<String, Object> getBanCostInfo(int totalPoints) {
        List<Map<String, Object>> slots = new ArrayList<>();
        for (int i = 0; i < MAX_BAN_SLOTS; i++) {
            int cost = BAN_COSTS[i];
            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("slot", i + 1);
            slot.put("cost", cost);
            slot.put("affordable", totalPoints >= cost);
            slots.add(slot);
        }

        return Map.of(
            "maxSlots", MAX_BAN_SLOTS,
            "slots", slots,
            "playerPoints", totalPoints
        );
    }

    /**
     * 计算 ban N 只宝可梦的总费用
     */
    public int calculateBanCost(int banCount) {
        if (banCount <= 0) return 0;
        if (banCount > MAX_BAN_SLOTS) banCount = MAX_BAN_SLOTS;

        int totalCost = 0;
        for (int i = 0; i < banCount; i++) {
            totalCost += BAN_COSTS[i];
        }
        return totalCost;
    }

    /**
     * 验证 ban 操作是否合法
     */
    public String validateBan(int banCount, int totalPoints) {
        if (banCount < 0 || banCount > MAX_BAN_SLOTS) {
            return "ban数量必须在0-" + MAX_BAN_SLOTS + "之间";
        }
        int cost = calculateBanCost(banCount);
        if (totalPoints < cost) {
            return "积分不足，需要" + cost + "积分，当前只有" + totalPoints + "积分";
        }
        return null; // 合法
    }

    /**
     * 扣除 ban 费用（返回扣除后的积分）
     */
    public int deductBanCost(int banCount, int totalPoints) {
        int cost = calculateBanCost(banCount);
        return Math.max(0, totalPoints - cost);
    }

    /**
     * 从队伍 JSON 中移除被 ban 的宝可梦
     */
    public List<Map<String, Object>> removeBannedPokemon(List<Map<String, Object>> team, Set<String> bannedNames) {
        if (bannedNames == null || bannedNames.isEmpty()) {
            return team;
        }

        List<Map<String, Object>> filtered = new ArrayList<>();
        Set<String> normalizedBanned = new HashSet<>();
        for (String name : bannedNames) {
            if (name != null) {
                normalizedBanned.add(name.toLowerCase());
            }
        }

        for (Map<String, Object> pokemon : team) {
            String name = String.valueOf(pokemon.getOrDefault("name_en", pokemon.getOrDefault("name", ""))).toLowerCase();
            if (!normalizedBanned.contains(name)) {
                filtered.add(pokemon);
            }
        }
        return filtered;
    }

    /**
     * 获取 ban 费用梯度描述
     */
    public static String getBanCostDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ban 费用梯度：\n");
        for (int i = 0; i < BAN_COSTS.length; i++) {
            sb.append("第").append(i + 1).append("只：");
            if (BAN_COSTS[i] == 0) {
                sb.append("免费");
            } else {
                sb.append(BAN_COSTS[i]).append("积分");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
