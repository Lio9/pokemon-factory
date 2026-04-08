package com.lio9.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.engine.BattleEngine;
import com.lio9.battle.mapper.BattleMapper;
import com.lio9.battle.mapper.BattleRoundMapper;
import com.lio9.battle.mapper.JobMapper;
import com.lio9.battle.mapper.OpponentPoolMapper;
import com.lio9.battle.mapper.PokemonMapper;
import com.lio9.battle.mapper.TeamMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步对战执行器。
 * <p>
 * 该组件负责把“自动模拟”模式转成后台任务：
 * 提交任务、恢复未完成任务、生成对手、运行 BattleEngine、落库回合日志和更新 job 状态。
 * </p>
 */
@Component
public class BattleExecutor {
    private static final int FACTORY_ROUND_LIMIT = 12;

    private final BattleMapper battleMapper;
    private final OpponentPoolMapper opponentPoolMapper;
    private final TeamMapper teamMapper;
    private final PokemonMapper pokemonMapper;
    private final BattleRoundMapper roundMapper;
    private final JobMapper jobMapper;
    private final BattleEngine engine;
    private final OpponentPoolService poolService;
    private final AIService aiService;
    private final ObjectMapper mapper;
    private ExecutorService executor;

    /**
     * 组装异步对战执行链依赖。
     */
    public BattleExecutor(BattleMapper battleMapper, OpponentPoolMapper opponentPoolMapper, TeamMapper teamMapper, PokemonMapper pokemonMapper, BattleRoundMapper roundMapper, JobMapper jobMapper, BattleEngine engine, OpponentPoolService poolService, AIService aiService, ObjectMapper mapper) {
        this.battleMapper = battleMapper;
        this.opponentPoolMapper = opponentPoolMapper;
        this.teamMapper = teamMapper;
        this.pokemonMapper = pokemonMapper;
        this.roundMapper = roundMapper;
        this.jobMapper = jobMapper;
        this.engine = engine;
        this.poolService = poolService;
        this.aiService = aiService;
        this.mapper = mapper;
    }

    /**
     * 初始化线程池，并尝试恢复进程重启前遗留的异步任务。
     */
    @PostConstruct
    public void init() {
        this.executor = Executors.newFixedThreadPool(2);
        recoverPendingJobs();
    }

    /**
     * 提交一场异步自动模拟任务。
     */
    public Integer submitAsyncBattle(Integer playerId, String playerTeamJson, String playerMoveMapJson) {
        try {
            String normalizedPlayerTeamJson = playerTeamJson;
            if (aiService.isBlankTeamJson(normalizedPlayerTeamJson)) {
                normalizedPlayerTeamJson = generatePlayerTeam(playerId);
            }
            final String finalPlayerTeamJson = normalizedPlayerTeamJson;

            battleMapper.insertInitial(playerId, null, 0, playerMoveMapJson, finalPlayerTeamJson, "queued", null, null);
            Integer battleId = battleMapper.lastInsertId();
            jobMapper.insertJob(battleId, "PENDING", playerMoveMapJson);
            executor.submit(() -> runBattle(battleId, playerId, finalPlayerTeamJson));
            return battleId;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * 真正执行一场后台自动战斗。
     */
    private void runBattle(Integer battleId, Integer playerId, String playerTeamJson) {
        try {
            long seed = System.currentTimeMillis() + battleId;
            Map<String, Object> opponent = resolveOpponent(seed, playerTeamJson);
            Integer opponentTeamId = (Integer) opponent.get("teamId");
            String opponentTeamJson = String.valueOf(opponent.get("teamJson"));

            Map<String, String> playerMoveMap = parseMoveMap(battleId);
            Map<String, Object> initialState = engine.createBattleState(playerTeamJson, opponentTeamJson, FACTORY_ROUND_LIMIT, seed);
            Map<String, Object> factory = initialState.containsKey("factory") && initialState.get("factory") instanceof Map ? (Map<String, Object>) initialState.get("factory") : new java.util.LinkedHashMap<>();
            factory.put("mode", "auto");
            factory.put("opponentSource", opponent.get("source"));
            initialState.put("factory", factory);

            battleMapper.updateBattleState(battleId, opponentTeamId, mapper.writeValueAsString(initialState), 0, String.valueOf(initialState.getOrDefault("phase", "battle")));
            Map<String, Object> finalState = engine.autoPlay(initialState, playerMoveMap);

            persistRounds(battleId, finalState);
            battleMapper.updateBattleTeamState(battleId, mapper.writeValueAsString(finalState.get("playerTeam")), mapper.writeValueAsString(finalState), String.valueOf(finalState.getOrDefault("phase", "completed")));
            Integer winnerPlayerId = "player".equals(finalState.get("winner")) ? playerId : null;
            battleMapper.updateBattle(battleId, opponentTeamId, mapper.writeValueAsString(finalState), rounds(finalState).size(), winnerPlayerId, String.valueOf(finalState.getOrDefault("phase", "completed")));

            if ("player".equals(finalState.get("winner"))) {
                Map<String, Object> latestTeam = teamMapper.findLatestByPlayer(playerId);
                if (latestTeam != null && latestTeam.get("id") != null) {
                    poolService.addTeamToPool(((Number) latestTeam.get("id")).intValue(), 0);
                }
            }

            markJobDone(battleId, "DONE");
        } catch (Exception e) {
            try {
                battleMapper.updateBattle(battleId, null, "{\"status\":\"completed\",\"phase\":\"completed\",\"winner\":\"opponent\",\"error\":\"executor_failed\"}", 0, null, "completed");
                markJobDone(battleId, "FAILED");
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 启动时重新拉起数据库里仍处于待执行状态的任务。
     */
    private void recoverPendingJobs() {
        try {
            List<Map<String, Object>> pending = jobMapper.findPendingJobs();
            if (pending == null) {
                return;
            }
            for (Map<String, Object> row : pending) {
                Integer battleId = row.get("battle_id") == null ? null : ((Number) row.get("battle_id")).intValue();
                if (battleId == null) {
                    continue;
                }
                try {
                    Map<String, Object> battle = battleMapper.findBattleWithOpponent(battleId.longValue());
                    if (battle == null || battle.isEmpty()) {
                        continue;
                    }
                    Integer playerId = battle.get("player_id") == null ? null : ((Number) battle.get("player_id")).intValue();
                    String playerTeamJson = String.valueOf(battle.getOrDefault("player_team_json", "[]"));
                    executor.submit(() -> runBattle(battleId, playerId, playerTeamJson));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取或生成玩家当前应使用的队伍。
     */
    private String generatePlayerTeam(Integer playerId) {
        Map<String, Object> existing = teamMapper.findLatestByPlayer(playerId);
        if (existing != null && existing.get("team_json") != null && !aiService.isBlankTeamJson(String.valueOf(existing.get("team_json")))) {
            return String.valueOf(existing.get("team_json"));
        }
        String generated = aiService.generateFactoryTeamJson(6, 0, System.currentTimeMillis(), Set.of());
        teamMapper.insertTeam(playerId, "factory-auto-" + System.currentTimeMillis(), generated, "generated");
        return generated;
    }

    /**
     * 获取异步模拟使用的对手队伍。
     */
    private Map<String, Object> resolveOpponent(long seed, String playerTeamJson) {
        List<Map<String, Object>> poolCandidates = poolService.sample(0, 2, 3);
        if (poolCandidates != null) {
            for (Map<String, Object> candidate : poolCandidates) {
                String teamJson = String.valueOf(candidate.get("team_json"));
                if (!aiService.extractNames(teamJson).equals(aiService.extractNames(playerTeamJson))) {
                    return Map.of("teamId", candidate.get("team_id"), "teamJson", teamJson, "source", "pool");
                }
            }
        }

        String generated = aiService.generateFactoryTeamJson(6, 1, seed, aiService.extractNames(playerTeamJson));
        teamMapper.insertTeam(null, "factory-auto-opponent-" + seed, generated, "generated");
        return Map.of("teamId", teamMapper.lastInsertId(), "teamJson", generated, "source", "generated");
    }

    @SuppressWarnings("unchecked")
    /**
     * 从 battle 表中恢复玩家预设动作。
     */
    private Map<String, String> parseMoveMap(Integer battleId) {
        try {
            Map<String, Object> row = battleMapper.findBattleWithOpponent(battleId.longValue());
            if (row == null || row.get("player_move_map") == null) {
                return Map.of();
            }
            Object rawMoveMap = row.get("player_move_map");
            if (rawMoveMap instanceof String rawString && !rawString.isBlank()) {
                return mapper.readValue(rawString, Map.class);
            }
        } catch (Exception ignored) {
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    /**
     * 从状态对象中安全读取 rounds 列表。
     */
    private List<Map<String, Object>> rounds(Map<String, Object> state) {
        Object rounds = state.get("rounds");
        return rounds instanceof List ? (List<Map<String, Object>>) rounds : new ArrayList<>();
    }

    /**
     * 把自动战斗产出的所有回合日志写入 battle_round。
     */
    private void persistRounds(Integer battleId, Map<String, Object> state) {
        for (Map<String, Object> round : rounds(state)) {
            try {
                roundMapper.insertRound(battleId, ((Number) round.getOrDefault("round", 0)).intValue(), mapper.writeValueAsString(round));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 更新异步任务状态。
     */
    private void markJobDone(Integer battleId, String status) {
        try {
            List<Map<String, Object>> jobs = jobMapper.findPendingJobs();
            if (jobs == null) {
                return;
            }
            for (Map<String, Object> job : jobs) {
                Integer queuedBattleId = job.get("battle_id") == null ? null : ((Number) job.get("battle_id")).intValue();
                Integer jobId = job.get("id") == null ? null : ((Number) job.get("id")).intValue();
                if (queuedBattleId != null && queuedBattleId.equals(battleId) && jobId != null) {
                    jobMapper.updateJobStatus(jobId, status);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
