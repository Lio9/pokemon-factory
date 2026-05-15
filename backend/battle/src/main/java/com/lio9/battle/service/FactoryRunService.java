package com.lio9.battle.service;



import com.lio9.battle.mapper.FactoryRunMapper;
import com.lio9.battle.mapper.PlayerMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工厂挑战服务：管理 9 轮连续对战的完整流程。
 * <p>
 * 流程：开始挑战 → 生成队伍 → 逐轮对战 → 胜利可交换 → 9 轮结束 → 结算积分
 * </p>
 */
@Service
public class FactoryRunService {
    private static final int MAX_BATTLES = 9;

    private final FactoryRunMapper runMapper;
    private final PlayerMapper playerMapper;
    private final BattleService battleService;
    private final AIService aiService;

    public FactoryRunService(FactoryRunMapper runMapper, PlayerMapper playerMapper, BattleService battleService, AIService aiService) {
        this.runMapper = runMapper;
        this.playerMapper = playerMapper;
        this.battleService = battleService;
        this.aiService = aiService;
    }

    /**
     * 开始一次新的工厂挑战。
     * <p>
     * 若玩家已有 active run，则直接返回恢复态，避免并发创建多条进行中记录。
     * </p>
     *
     * @param username 玩家用户名
     * @return run 初始化信息或已存在 run 的恢复信息
     */
    public Map<String, Object> startRun(String username) {
        playerMapper.insertIgnore(username);
        Integer playerId = playerMapper.findIdByUsername(username);
        Map<String, Object> profile = playerMapper.findByUsername(username);
        int tier = toInt(profile.get("tier"), 0);

        // 检查是否已有活跃的工厂挑战
        Map<String, Object> existingRun = runMapper.findActiveRun(playerId);
        if (existingRun != null) {
            return resumeRun(existingRun, username, profile);
        }

        // 生成初始 6 只队伍
        long seed = System.currentTimeMillis();
        String teamJson = aiService.generateFactoryTeamJson(6, tier, seed, Set.of());

        runMapper.insertRun(playerId, MAX_BATTLES, teamJson, tier);
        Integer runId = runMapper.lastInsertId();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("runId", runId);
        response.put("status", "active");
        response.put("currentBattle", 0);
        response.put("maxBattles", MAX_BATTLES);
        response.put("wins", 0);
        response.put("losses", 0);
        response.put("teamJson", teamJson);
        response.put("tier", tier);
        response.put("tierName", TierService.tierName(tier));
        response.put("profile", buildProfileSummary(profile));
        response.put("message", "工厂挑战开始！你将进行 " + MAX_BATTLES + " 轮连续对战。");
        return response;
    }

    /**
     * 恢复未完成的工厂挑战。
     */
    private Map<String, Object> resumeRun(Map<String, Object> run, String username, Map<String, Object> profile) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("runId", run.get("id"));
        response.put("status", run.get("status"));
        response.put("currentBattle", toInt(run.get("current_battle"), 0));
        response.put("maxBattles", toInt(run.get("max_battles"), MAX_BATTLES));
        response.put("wins", toInt(run.get("wins"), 0));
        response.put("losses", toInt(run.get("losses"), 0));
        response.put("teamJson", run.get("team_json"));
        response.put("currentBattleId", run.get("current_battle_id"));
        response.put("tier", toInt(profile.get("tier"), 0));
        response.put("tierName", TierService.tierName(toInt(profile.get("tier"), 0)));
        response.put("profile", buildProfileSummary(profile));
        response.put("message", "你有一个未完成的工厂挑战，已恢复进度。");
        return response;
    }

    /**
     * 在工厂挑战中开始下一场对战。
     * <p>
     * 该方法会复用 run 中沿用队伍并委托 BattleService.startMatch 创建 battle。
     * run 的 currentBattle/currentBattleId 也会同步推进。
     * </p>
     *
     * @param username 玩家用户名
     * @param runId 工厂挑战 ID
     * @return 新一轮 battle 初始化结果，或 run_not_active/not_your_run 等错误
     */
    public Map<String, Object> startNextBattle(String username, Integer runId) {
        playerMapper.insertIgnore(username);
        Integer playerId = playerMapper.findIdByUsername(username);
        Map<String, Object> profile = playerMapper.findByUsername(username);

        Map<String, Object> run = runMapper.findById(runId);
        if (run == null || !"active".equals(run.get("status"))) {
            return Map.of("error", "run_not_active", "message", "该工厂挑战已结束或不存在。");
        }
        if (!playerId.equals(toInt(run.get("player_id"), -1))) {
            return Map.of("error", "not_your_run");
        }

        int currentBattle = toInt(run.get("current_battle"), 0);
        int maxBattles = toInt(run.get("max_battles"), MAX_BATTLES);
        if (currentBattle >= maxBattles) {
            return finishRun(runId, run, profile);
        }

        // 使用工厂挑战中沿用的队伍
        String teamJson = String.valueOf(run.get("team_json"));

        // 构建请求交给现有的 battleService.startMatch
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("username", username);
        req.put("teamJson", teamJson);
        req.put("factoryRunId", runId);
        req.put("runBattleNumber", currentBattle + 1);

        Map<String, Object> battleResult = battleService.startMatch(req);
        if (battleResult.containsKey("error")) {
            return battleResult;
        }

        Integer battleId = (Integer) battleResult.get("battleId");
        runMapper.updateProgress(runId, currentBattle + 1, toInt(run.get("wins"), 0), toInt(run.get("losses"), 0), battleId, teamJson);

        Map<String, Object> response = new LinkedHashMap<>(battleResult);
        response.put("runId", runId);
        response.put("runBattle", currentBattle + 1);
        response.put("maxBattles", maxBattles);
        response.put("runWins", toInt(run.get("wins"), 0));
        response.put("runLosses", toInt(run.get("losses"), 0));
        return response;
    }

    /**
     * 单场对战结束后更新工厂挑战进度。
     * <p>
     * 由 BattleService 在对战结束时回调。该方法只维护 run 内统计，
     * 达到终止条件时再统一触发 finishRun 做积分结算。
     * </p>
     *
     * @param factoryRunId 工厂挑战 ID
     * @param playerWon 是否玩家获胜
     * @param updatedTeamJson 胜利交换后可能变更的最新队伍 JSON
     */
    public void onBattleCompleted(Integer factoryRunId, boolean playerWon, String updatedTeamJson) {
        if (factoryRunId == null) return;

        Map<String, Object> run = runMapper.findById(factoryRunId);
        if (run == null || !"active".equals(run.get("status"))) return;

        int wins = toInt(run.get("wins"), 0) + (playerWon ? 1 : 0);
        int losses = toInt(run.get("losses"), 0) + (playerWon ? 0 : 1);
        int currentBattle = toInt(run.get("current_battle"), 0);
        String teamJson = updatedTeamJson != null ? updatedTeamJson : String.valueOf(run.get("team_json"));

        runMapper.updateProgress(factoryRunId, currentBattle, wins, losses, null, teamJson);

        // 检查是否达到终止条件
        int maxBattles = toInt(run.get("max_battles"), MAX_BATTLES);
        if (currentBattle >= maxBattles) {
            Map<String, Object> playerProfile = findPlayerByRunId(run);
            finishRun(factoryRunId, run, playerProfile);
        }
    }

    /**
     * 结束工厂挑战并结算积分。
     * <p>
     * 积分按 run 全局战绩计算，结算后写回 tier/tierPoints/totalPoints 与总胜负。
     * </p>
     *
     * @param runId 工厂挑战 ID
     * @param run run 行数据
     * @param profile 玩家资料快照
     * @return 结算结果（包含升降段位信息）
     */
    public Map<String, Object> finishRun(Integer runId, Map<String, Object> run, Map<String, Object> profile) {
        int wins = toInt(run.get("wins"), 0);
        int losses = toInt(run.get("losses"), 0);
        int tier = toInt(profile.get("tier"), 0);
        int tierPoints = toInt(profile.get("tier_points"), 0);
        int totalPoints = toInt(profile.get("total_points"), 0);
        int highestTier = toInt(profile.get("highest_tier"), 0);
        int totalWins = toInt(profile.get("wins"), 0);
        int totalLosses = toInt(profile.get("losses"), 0);

        int reward = TierService.calculateRunReward(tier, wins, losses);
        Map<String, Object> tierResult = TierService.applyPoints(tier, tierPoints, totalPoints, reward);

        int newTier = (int) tierResult.get("tier");
        int newTierPoints = (int) tierResult.get("tierPoints");
        int newTotalPoints = (int) tierResult.get("totalPoints");
        int newHighestTier = Math.max(highestTier, newTier);

        Integer playerId = toInt(run.get("player_id"), 0);
        playerMapper.updateTierProgress(playerId, newTier, newTierPoints, newTotalPoints, newHighestTier, totalWins + wins, totalLosses + losses);
        runMapper.finishRun(runId, "completed", reward);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("runId", runId);
        response.put("status", "completed");
        response.put("wins", wins);
        response.put("losses", losses);
        response.put("pointsEarned", reward);
        response.put("tierBefore", TierService.tierName(tier));
        response.put("tierAfter", TierService.tierName(newTier));
        response.put("tierPointsBefore", tierPoints);
        response.put("tierPointsAfter", newTierPoints);
        response.put("totalPoints", newTotalPoints);
        response.put("promoted", tierResult.get("promoted"));
        response.put("demoted", tierResult.get("demoted"));
        response.put("message", String.format("工厂挑战结束！战绩 %d胜%d负，获得 %d 积分。", wins, losses, reward));
        return response;
    }

    /**
     * 主动结束（放弃）当前工厂挑战。
     *
     * @param username 玩家用户名
     * @return 无 active run 返回 no_active_run；否则立即走 finishRun 结算
     */
    public Map<String, Object> abandonRun(String username) {
        playerMapper.insertIgnore(username);
        Integer playerId = playerMapper.findIdByUsername(username);
        Map<String, Object> run = runMapper.findActiveRun(playerId);
        if (run == null) {
            return Map.of("error", "no_active_run", "message", "没有进行中的工厂挑战。");
        }

        Map<String, Object> profile = playerMapper.findByUsername(username);
        return finishRun(toInt(run.get("id"), 0), run, profile);
    }

    /**
     * 获取当前工厂挑战状态。
     *
     * @param username 玩家用户名
     * @return 包含 profile 与 activeRun（若存在）
     */
    public Map<String, Object> getRunStatus(String username) {
        playerMapper.insertIgnore(username);
        Integer playerId = playerMapper.findIdByUsername(username);
        Map<String, Object> profile = playerMapper.findByUsername(username);
        Map<String, Object> run = runMapper.findActiveRun(playerId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profile", buildProfileSummary(profile));
        if (run != null) {
            response.put("activeRun", run);
        }
        return response;
    }

    /**
     * 玩家信息摘要。
     *
     * @param username 玩家用户名
     * @return 包含 profile、activeRun、最近对战与最近 run 列表
     */
    public Map<String, Object> getProfile(String username) {
        playerMapper.insertIgnore(username);
        Integer playerId = playerMapper.findIdByUsername(username);
        Map<String, Object> profile = playerMapper.findByUsername(username);
        Map<String, Object> run = runMapper.findActiveRun(playerId);
        List<Map<String, Object>> history = playerMapper.findBattleHistory(playerId, 20);
        List<Map<String, Object>> recentRuns = runMapper.findRecentRuns(playerId, 10);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profile", buildProfileSummary(profile));
        response.put("activeRun", run);
        response.put("recentBattles", history);
        response.put("recentRuns", recentRuns);
        return response;
    }

    /**
     * 排行榜。
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        List<Map<String, Object>> entries = playerMapper.leaderboard(Math.min(limit, 100));
        for (Map<String, Object> entry : entries) {
            entry.put("tierName", TierService.tierName(toInt(entry.get("tier"), 0)));
        }
        return entries;
    }

    private Map<String, Object> buildProfileSummary(Map<String, Object> profile) {
        if (profile == null) return Map.of();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("username", profile.get("username"));
        summary.put("tier", toInt(profile.get("tier"), 0));
        summary.put("tierName", TierService.tierName(toInt(profile.get("tier"), 0)));
        summary.put("tierPoints", toInt(profile.get("tier_points"), 0));
        summary.put("totalPoints", toInt(profile.get("total_points"), 0));
        summary.put("highestTier", toInt(profile.get("highest_tier"), 0));
        summary.put("highestTierName", TierService.tierName(toInt(profile.get("highest_tier"), 0)));
        summary.put("wins", toInt(profile.get("wins"), 0));
        summary.put("losses", toInt(profile.get("losses"), 0));
        summary.put("nextTierPoints", TierService.POINTS_PER_TIER);
        return summary;
    }

    private Map<String, Object> findPlayerByRunId(Map<String, Object> run) {
        Integer playerId = toInt(run.get("player_id"), 0);
        Map<String, Object> profile = playerMapper.findById(playerId);
        if (profile != null) return profile;
        // 兜底：使用 run 中记录的初始段位
        return Map.of("tier", toInt(run.get("tier_at_start"), 0), "tier_points", 0, "total_points", 0, "highest_tier", 0, "wins", 0, "losses", 0);
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        if (value != null) {
            try { return Integer.parseInt(value.toString()); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }
}
