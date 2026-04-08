package com.lio9.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.engine.BattleEngine;
import com.lio9.battle.mapper.BattleMapper;
import com.lio9.battle.mapper.BattleRoundMapper;
import com.lio9.battle.mapper.PlayerMapper;
import com.lio9.battle.mapper.PokemonMapper;
import com.lio9.battle.mapper.TeamMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 手动对战业务编排服务。
 * <p>
 * 主要职责：
 * 1. 组装玩家队伍与对手队伍；
 * 2. 调用 BattleEngine 推进预览、补位和回合逻辑；
 * 3. 把 battle / battle_round / player / team 等表的状态统一落库；
 * 4. 在对战结束后更新玩家积分与对手池。
 * </p>
 */
@Service
public class BattleService {
    private static final int FACTORY_TEAM_SIZE = 6;
    private static final int FACTORY_ROUND_LIMIT = 12;

    private final PlayerMapper playerMapper;
    private final TeamMapper teamMapper;
    private final PokemonMapper pokemonMapper;
    private final BattleMapper battleMapper;
    private final BattleRoundMapper roundMapper;
    private final OpponentPoolService poolService;
    private final BattleEngine battleEngine;
    private final AIService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 组装手动对战主链依赖。
     */
    public BattleService(PlayerMapper playerMapper, TeamMapper teamMapper, PokemonMapper pokemonMapper, BattleMapper battleMapper, BattleRoundMapper roundMapper, BattleEngine battleEngine, OpponentPoolService poolService, AIService aiService) {
        this.playerMapper = playerMapper;
        this.teamMapper = teamMapper;
        this.pokemonMapper = pokemonMapper;
        this.battleMapper = battleMapper;
        this.roundMapper = roundMapper;
        this.battleEngine = battleEngine;
        this.poolService = poolService;
        this.aiService = aiService;
    }

    /**
     * 开始一场手动对战，并返回预览阶段状态。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> startMatch(Map<String, Object> req) {
        String username = String.valueOf(req.getOrDefault("username", "guest"));
        playerMapper.insertIgnore(username);
        Integer playerId = playerMapper.findIdByUsername(username);
        Map<String, Object> playerProfile = playerMapper.findByUsername(username);
        int playerRank = playerProfile == null ? 0 : toInt(playerProfile.get("rank"), 0);

        long seed = System.currentTimeMillis();
        String requestedTeamJson = req.get("teamJson") instanceof String ? req.get("teamJson").toString() : null;
        Map<String, Object> playerTeam = resolvePlayerTeam(playerId, playerRank, seed, requestedTeamJson);
        String playerTeamJson = String.valueOf(playerTeam.get("teamJson"));
        Integer playerTeamId = (Integer) playerTeam.get("teamId");

        Map<String, Object> opponentTeam = resolveOpponentTeam(playerRank, seed + 97, aiService.extractNames(playerTeamJson));
        String opponentTeamJson = String.valueOf(opponentTeam.get("teamJson"));
        Integer opponentTeamId = (Integer) opponentTeam.get("teamId");

        Map<String, String> playerMoveMap = parsePlayerMoveMap(req.get("playerMoveMap"));
        Map<String, Object> state = battleEngine.createPreviewState(playerTeamJson, opponentTeamJson, FACTORY_ROUND_LIMIT, seed);
        enrichStateMetadata(state, "manual", username, playerRank, String.valueOf(opponentTeam.get("source")));

        try {
            battleMapper.insertInitial(playerId, opponentTeamId, 0, toJson(playerMoveMap), playerTeamJson, String.valueOf(state.get("phase")));
            Integer battleId = battleMapper.lastInsertId();
            battleMapper.updateBattleState(battleId, opponentTeamId, toJson(state), 0, String.valueOf(state.get("phase")));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("battleId", battleId);
            response.put("status", state.get("status"));
            response.put("summary", state);
            response.put("playerTeamJson", playerTeamJson);
            response.put("opponentTeamJson", opponentTeamJson);
            response.put("playerTeamId", playerTeamId);
            response.put("opponentTeamId", opponentTeamId);
            response.put("source", opponentTeam.get("source"));
            response.put("message", "对战工厂匹配完成，请先完成队伍预览和首发选择。");
            return response;
        } catch (Exception e) {
            return Map.of("error", "persist_failed", "detail", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * 查询对战状态，并在可能时把 summary_json 解析成结构化对象返回给前端。
     */
    public Map<String, Object> getBattleStatus(Long battleId) {
        Map<String, Object> row = battleMapper.findBattleWithOpponent(battleId);
        if (row == null || row.isEmpty()) {
            return Map.of("error", "not_found");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("battle", row);
        if (row.get("summary_json") instanceof String summaryJson && !summaryJson.isBlank()) {
            try {
                response.put("summary", objectMapper.readValue(summaryJson, Map.class));
            } catch (Exception ignored) {
            }
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    /**
     * 提交一回合玩家动作并推进战斗。
     */
    public Map<String, Object> applyMove(Long battleId, Map<String, String> playerMoveMap) {
        try {
            Map<String, Object> row = battleMapper.findBattleWithOpponent(battleId);
            if (row == null || row.isEmpty()) {
                return Map.of("error", "not_found");
            }

            Map<String, Object> existingState = parseSummaryState(row.get("summary_json"));
            if (existingState.isEmpty()) {
                existingState = battleEngine.createBattleState(
                        String.valueOf(row.getOrDefault("player_team_json", "[]")),
                        String.valueOf(row.getOrDefault("opponent_team_json", "[]")),
                        FACTORY_ROUND_LIMIT,
                        System.currentTimeMillis()
                );
            }

            int previousRounds = roundsCount(existingState);
            boolean wasCompleted = "completed".equals(existingState.get("status"));
            if (wasCompleted) {
                return Map.of("status", "completed", "summary", existingState);
            }
            if ("preview".equals(existingState.get("status")) || "team-preview".equals(existingState.get("phase"))) {
                return Map.of("error", "preview_required", "summary", existingState, "message", "请先确认 6 选 4 和 2 只首发。");
            }
            if ("replacement".equals(existingState.get("phase"))) {
                return Map.of("error", "replacement_required", "summary", existingState, "message", "请先为倒下的宝可梦选择替补。");
            }

            Map<String, Object> updatedState = battleEngine.playRound(existingState, playerMoveMap == null ? Map.of() : playerMoveMap);
            persistNewRounds((int) (long) battleId, existingState, updatedState);

            Integer opponentTeamId = row.get("opponent_team_id") == null ? null : ((Number) row.get("opponent_team_id")).intValue();
            String playerTeamJson = toJson(updatedState.get("playerTeam"));
            String phase = String.valueOf(updatedState.getOrDefault("phase", updatedState.getOrDefault("status", "battle")));
            battleMapper.updateBattleState(battleId.intValue(), opponentTeamId, toJson(updatedState), roundsCount(updatedState), phase);
            battleMapper.updateBattleTeamState(battleId.intValue(), playerTeamJson, toJson(updatedState), phase);

            if ("completed".equals(updatedState.get("status"))) {
                finalizePlayerProgress(row, updatedState);
                Integer winnerPlayerId = "player".equals(updatedState.get("winner")) && row.get("player_id") != null ? ((Number) row.get("player_id")).intValue() : null;
                battleMapper.updateBattle(battleId.intValue(), opponentTeamId, toJson(updatedState), roundsCount(updatedState), winnerPlayerId, String.valueOf(updatedState.getOrDefault("phase", "completed")));
                if ("player".equals(updatedState.get("winner"))) {
                    Map<String, Object> latestTeam = teamMapper.findLatestByPlayer(((Number) row.get("player_id")).intValue());
                    if (latestTeam != null && latestTeam.get("id") != null) {
                        poolService.addTeamToPool(((Number) latestTeam.get("id")).intValue(), toInt(extractFactory(updatedState).get("playerRank"), 0));
                    }
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", updatedState.get("status"));
            response.put("summary", updatedState);
            response.put("battleId", battleId);
            response.put("newRounds", roundsCount(updatedState) - previousRounds);
            return response;
        } catch (Exception e) {
            return Map.of("error", "apply_failed", "detail", e.getMessage());
        }
    }

    /**
     * 确认补位选择。
     */
    public Map<String, Object> confirmReplacement(Long battleId, Map<String, Object> selection) {
        try {
            Map<String, Object> row = battleMapper.findBattleWithOpponent(battleId);
            if (row == null || row.isEmpty()) {
                return Map.of("error", "not_found");
            }

            Map<String, Object> existingState = parseSummaryState(row.get("summary_json"));
            if (existingState.isEmpty()) {
                return Map.of("error", "battle_state_missing");
            }
            if (!"replacement".equals(existingState.get("phase"))) {
                return Map.of("error", "replacement_closed", "summary", existingState);
            }

            Map<String, Object> updatedState = battleEngine.applyReplacementSelection(existingState, selection);
            Integer opponentTeamId = row.get("opponent_team_id") == null ? null : ((Number) row.get("opponent_team_id")).intValue();
            String playerTeamJson = toJson(updatedState.get("playerTeam"));
            String phase = String.valueOf(updatedState.getOrDefault("phase", "battle"));

            battleMapper.updateBattleState(battleId.intValue(), opponentTeamId, toJson(updatedState), roundsCount(updatedState), phase);
            battleMapper.updateBattleTeamState(battleId.intValue(), playerTeamJson, toJson(updatedState), phase);

            return Map.of(
                    "battleId", battleId,
                    "status", updatedState.get("status"),
                    "summary", updatedState,
                    "message", "替补上场已确认。"
            );
        } catch (IllegalArgumentException e) {
            return Map.of("error", e.getMessage(), "message", "替补选择无效。");
        } catch (Exception e) {
            return Map.of("error", "replacement_confirm_failed", "detail", e.getMessage());
        }
    }

    /**
     * 抽样读取当前段位附近的对手池。
     */
    public List<Map<String, Object>> samplePool(Integer rank) {
        int targetRank = rank == null ? 0 : rank;
        return poolService.sample(targetRank, 2, 5);
    }

    /**
     * 确认队伍预览阶段选择，并正式进入 battle 阶段。
     */
    public Map<String, Object> confirmTeamPreview(Long battleId, Map<String, Object> selection) {
        try {
            Map<String, Object> row = battleMapper.findBattleWithOpponent(battleId);
            if (row == null || row.isEmpty()) {
                return Map.of("error", "not_found");
            }

            Map<String, Object> existingState = parseSummaryState(row.get("summary_json"));
            if (existingState.isEmpty()) {
                return Map.of("error", "battle_state_missing");
            }
            if (!"preview".equals(existingState.get("status")) && !"team-preview".equals(existingState.get("phase"))) {
                return Map.of("error", "preview_closed", "summary", existingState);
            }

            Map<String, Object> updatedState = battleEngine.applyTeamPreviewSelection(existingState, selection, null);
            Integer opponentTeamId = row.get("opponent_team_id") == null ? null : ((Number) row.get("opponent_team_id")).intValue();
            String playerTeamJson = toJson(updatedState.get("playerTeam"));
            String phase = String.valueOf(updatedState.getOrDefault("phase", "battle"));

            battleMapper.updateBattleState(battleId.intValue(), opponentTeamId, toJson(updatedState), 0, phase);
            battleMapper.updateBattleTeamState(battleId.intValue(), playerTeamJson, toJson(updatedState), phase);

            return Map.of(
                    "battleId", battleId,
                    "status", updatedState.get("status"),
                    "summary", updatedState,
                    "message", "队伍预览已确认，对战开始。"
            );
        } catch (Exception e) {
            return Map.of("error", "preview_confirm_failed", "detail", e.getMessage());
        }
    }

    /**
     * 处理胜利后的交换奖励逻辑，并把变更后的队伍重新写回玩家当前队伍。
     */
    public Map<String, Object> updateBattleAfterExchange(Long battleId, int replacedIndex, String newPokemonJson) {
        try {
            Map<String, Object> battleStatus = getBattleStatus(battleId);
            if (battleStatus.containsKey("error")) {
                return battleStatus;
            }

            Map<String, Object> battle = castMap(battleStatus.get("battle"));
            Map<String, Object> state = castMap(battleStatus.get("summary"));
            if (!"completed".equals(state.get("status")) || !"player".equals(state.get("winner")) || !Boolean.TRUE.equals(state.get("exchangeAvailable"))) {
                return Map.of("error", "exchange_not_available");
            }

            Map<String, Object> latestTeam = teamMapper.findLatestByPlayer(((Number) battle.get("player_id")).intValue());
            if (latestTeam == null || latestTeam.get("id") == null) {
                return Map.of("error", "team_not_found");
            }

            int playerTeamId = ((Number) latestTeam.get("id")).intValue();
            int version = toInt(latestTeam.get("version"), 0);
            List<Map<String, Object>> playerTeam = aiService.parseTeam(String.valueOf(latestTeam.get("team_json")));
            if (replacedIndex < 0 || replacedIndex >= playerTeam.size()) {
                return Map.of("error", "invalid_replaced_index");
            }

            Map<String, Object> replacement = castMap(objectMapper.readValue(newPokemonJson, Map.class));
            Map<String, Object> replaced = playerTeam.set(replacedIndex, replacement);
            String updatedTeamJson = toJson(playerTeam);

            if (teamMapper.updateTeamWithVersion(playerTeamId, updatedTeamJson, version) == 0) {
                return Map.of("error", "team_stale");
            }

            Map<String, Object> updatedState = battleEngine.replacePlayerTeamMember(state, replacedIndex, replacement);
            battleMapper.updateBattleTeamState(battleId.intValue(), updatedTeamJson, toJson(updatedState), String.valueOf(updatedState.getOrDefault("phase", updatedState.getOrDefault("status", "completed"))));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "ok");
            response.put("summary", updatedState);
            response.put("replacedPokemon", replaced);
            return response;
        } catch (Exception e) {
            return Map.of("error", "exchange_failed", "detail", e.getMessage());
        }
    }

    /**
     * 把对战工厂模式下需要展示的额外元信息写进 summary.factory。
     */
    private void enrichStateMetadata(Map<String, Object> state, String mode, String username, int playerRank, String opponentSource) {
        Map<String, Object> factory = extractFactory(state);
        factory.put("mode", mode);
        factory.put("username", username);
        factory.put("playerRank", playerRank);
        factory.put("opponentSource", opponentSource);
        factory.put("teamSize", FACTORY_TEAM_SIZE);
    }

    /**
     * 对战结束后结算玩家积分和段位。
     */
    private void finalizePlayerProgress(Map<String, Object> battleRow, Map<String, Object> state) {
        Integer playerId = battleRow.get("player_id") == null ? null : ((Number) battleRow.get("player_id")).intValue();
        if (playerId == null) {
            return;
        }

        Map<String, Object> factory = extractFactory(state);
        if (Boolean.TRUE.equals(factory.get("progressApplied"))) {
            return;
        }

        Map<String, Object> playerProfile = playerMapper.findByUsername(String.valueOf(factory.getOrDefault("username", "")));
        if (playerProfile == null || playerProfile.isEmpty()) {
            return;
        }

        int points = toInt(playerProfile.get("points"), 0);
        points = "player".equals(state.get("winner")) ? points + 10 : Math.max(0, points - 3);
        int rank = points / 30;
        playerMapper.updateProgress(playerId, rank, points);
        factory.put("progressApplied", true);
        factory.put("playerRank", rank);
        factory.put("playerPoints", points);
    }

    /**
     * 只把新增出来的回合日志补写到 battle_round，避免重复插入旧回合。
     */
    private void persistNewRounds(int battleId, Map<String, Object> oldState, Map<String, Object> newState) {
        List<Map<String, Object>> oldRounds = extractRounds(oldState);
        List<Map<String, Object>> newRounds = extractRounds(newState);
        for (int index = oldRounds.size(); index < newRounds.size(); index++) {
            Map<String, Object> round = newRounds.get(index);
            try {
                roundMapper.insertRound(battleId, toInt(round.get("round"), index + 1), toJson(round));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 解析玩家当前应使用的队伍。
     * <p>
     * 优先级依次为：请求自带队伍 > 玩家最近一次队伍 > 系统自动生成队伍。
     * </p>
     */
    private Map<String, Object> resolvePlayerTeam(Integer playerId, int playerRank, long seed, String requestedTeamJson) {
        if (!aiService.isBlankTeamJson(requestedTeamJson)) {
            teamMapper.insertTeam(playerId, "custom-" + seed, requestedTeamJson, "player");
            return Map.of("teamId", teamMapper.lastInsertId(), "teamJson", requestedTeamJson, "source", "custom");
        }

        Map<String, Object> existing = teamMapper.findLatestByPlayer(playerId);
        if (existing != null && existing.get("team_json") != null && !aiService.isBlankTeamJson(String.valueOf(existing.get("team_json")))) {
            return Map.of("teamId", existing.get("id"), "teamJson", existing.get("team_json"), "source", "stored");
        }

        String generatedTeamJson = aiService.generateFactoryTeamJson(FACTORY_TEAM_SIZE, playerRank, seed, Set.of());
        teamMapper.insertTeam(playerId, "factory-player-" + seed, generatedTeamJson, "generated");
        return Map.of("teamId", teamMapper.lastInsertId(), "teamJson", generatedTeamJson, "source", "generated");
    }

    /**
     * 解析本场对手队伍。
     * <p>
     * 优先从对手池抽取，若抽不到合适结果再退回到 AI 自动生成。
     * </p>
     */
    private Map<String, Object> resolveOpponentTeam(int playerRank, long seed, Set<String> excludedNames) {
        List<Map<String, Object>> poolCandidates = poolService.sample(playerRank, 2, 3);
        if (poolCandidates != null) {
            for (Map<String, Object> candidate : poolCandidates) {
                String teamJson = String.valueOf(candidate.get("team_json"));
                if (!aiService.extractNames(teamJson).equals(excludedNames)) {
                    return Map.of(
                            "teamId", candidate.get("team_id"),
                            "teamJson", teamJson,
                            "source", "pool"
                    );
                }
            }
        }

        String generatedTeamJson = aiService.generateFactoryTeamJson(FACTORY_TEAM_SIZE, Math.max(0, playerRank + 1), seed, excludedNames);
        teamMapper.insertTeam(null, "factory-opponent-" + seed, generatedTeamJson, "generated");
        return Map.of("teamId", teamMapper.lastInsertId(), "teamJson", generatedTeamJson, "source", "generated");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSummaryState(Object summaryJson) {
        if (!(summaryJson instanceof String summary) || summary.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(summary, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRounds(Map<String, Object> state) {
        Object rounds = state.get("rounds");
        return rounds instanceof List ? (List<Map<String, Object>>) rounds : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFactory(Map<String, Object> state) {
        Object factory = state.get("factory");
        if (factory instanceof Map) {
            return (Map<String, Object>) factory;
        }
        Map<String, Object> created = new LinkedHashMap<>();
        state.put("factory", created);
        return created;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parsePlayerMoveMap(Object rawValue) {
        if (rawValue instanceof Map<?, ?> rawMap) {
            Map<String, String> moveMap = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> moveMap.put(String.valueOf(key), value == null ? "" : String.valueOf(value)));
            return moveMap;
        }
        if (rawValue instanceof String rawString && !rawString.isBlank()) {
            try {
                return objectMapper.readValue(rawString, Map.class);
            } catch (Exception ignored) {
            }
        }
        return Map.of();
    }

    private int roundsCount(Map<String, Object> state) {
        return extractRounds(state).size();
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
