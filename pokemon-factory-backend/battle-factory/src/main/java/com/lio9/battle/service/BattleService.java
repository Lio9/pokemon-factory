package com.lio9.battle.service;

/**
 * BattleService 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端业务服务文件。
 * 核心职责：负责定义或承载模块级业务能力，对上层暴露稳定服务接口。
 * 阅读建议：建议结合控制器和实现类一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.config.BattleConfig;
import com.lio9.battle.engine.BattleEngine;
import com.lio9.battle.mapper.BattleMapper;
import com.lio9.battle.mapper.BattleExchangeMapper;
import com.lio9.battle.mapper.BattleRoundMapper;
import com.lio9.battle.mapper.PlayerMapper;
import com.lio9.battle.mapper.TeamMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(BattleService.class);
    private static final int FACTORY_TEAM_SIZE = 6;
    private static final int FACTORY_ROUND_LIMIT = 12;

    private final PlayerMapper playerMapper;
    private final TeamMapper teamMapper;
    private final BattleMapper battleMapper;
    private final BattleRoundMapper roundMapper;
    private final BattleExchangeMapper exchangeMapper;
    private final OpponentPoolService poolService;
    private final BattleEngine battleEngine;
    private final AIService aiService;
    private final FactoryRunService factoryRunService;
    private final ObjectMapper objectMapper;
    private final BattleConfig config;

    /**
     * 组装手动对战主链依赖。
     */
    public BattleService(PlayerMapper playerMapper, TeamMapper teamMapper, BattleMapper battleMapper,
            BattleRoundMapper roundMapper, BattleExchangeMapper exchangeMapper,
            BattleEngine battleEngine, OpponentPoolService poolService, AIService aiService,
            FactoryRunService factoryRunService, ObjectMapper objectMapper, BattleConfig config) {
        this.playerMapper = playerMapper;
        this.teamMapper = teamMapper;
        this.battleMapper = battleMapper;
        this.roundMapper = roundMapper;
        this.exchangeMapper = exchangeMapper;
        this.battleEngine = battleEngine;
        this.poolService = poolService;
        this.aiService = aiService;
        this.factoryRunService = factoryRunService;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    /**
     * 开始一场手动对战，并返回预览阶段状态。
     * <p>
     * 该入口会完成以下编排：
     * 1) 保障玩家账号存在并读取段位；
     * 2) 解析玩家队伍/抽取对手队伍；
     * 3) 构建 preview 状态并落库到 battle 主表；
     * 4) 返回前端进入队伍预览所需的完整上下文。
     * </p>
     *
     * @param req 请求体，支持
     *            username、teamJson、playerMoveMap、factoryRunId、runBattleNumber
     * @return 成功返回 battleId/summary/队伍信息；失败返回 error 与 detail
     */
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
        Integer playerTeamId = playerTeam.get("teamId") == null ? null : ((Number) playerTeam.get("teamId")).intValue();

        Map<String, Object> opponentTeam = resolveOpponentTeam(playerRank, seed + 97,
                aiService.extractNames(playerTeamJson));
        String opponentTeamJson = String.valueOf(opponentTeam.get("teamJson"));
        Integer opponentTeamId = opponentTeam.get("teamId") == null ? null
                : ((Number) opponentTeam.get("teamId")).intValue();

        Map<String, String> playerMoveMap = parsePlayerMoveMap(req.get("playerMoveMap"));
        Map<String, Object> state = battleEngine.createPreviewState(playerTeamJson, opponentTeamJson,
                FACTORY_ROUND_LIMIT, seed);
        enrichStateMetadata(state, "manual", username, playerRank, String.valueOf(opponentTeam.get("source")));

        Integer factoryRunId = req.get("factoryRunId") instanceof Number n ? n.intValue() : null;
        Integer runBattleNumber = req.get("runBattleNumber") instanceof Number n ? n.intValue() : null;
        if (factoryRunId != null) {
            Map<String, Object> factory = extractFactory(state);
            factory.put("factoryRunId", factoryRunId);
            factory.put("runBattleNumber", runBattleNumber);
        }

        try {
            battleMapper.insertInitial(playerId, opponentTeamId, 0, toJson(playerMoveMap), playerTeamJson,
                    String.valueOf(state.get("phase")), factoryRunId, runBattleNumber);
            Integer battleId = battleMapper.lastInsertId();
            battleMapper.updateBattleState(battleId, opponentTeamId, toJson(state), 0,
                    String.valueOf(state.get("phase")));

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
            log.error("创建对战失败, username={}, factoryRunId={}", username, factoryRunId, e);
            return Map.of("error", "persist_failed", "detail", e.getMessage());
        }
    }

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
                response.put("summary", objectMapper.readValue(summaryJson, new TypeReference<Map<String, Object>>() {
                }));
            } catch (Exception e) {
                log.warn("解析对战摘要失败, battleId={}", battleId, e);
            }
        }
        return response;
    }

    /**
     * 提交一回合玩家动作并推进战斗。
     * <p>
     * 该方法是手动对战主循环入口：
     * 会先校验当前 phase（preview/replacement/completed），再调用引擎推进 1 回合，
     * 最后只持久化新增 round 并在完成态触发积分结算与对手池维护。
     * </p>
     *
     * @param battleId      对战主键
     * @param playerMoveMap 玩家动作映射（可为空，空时按默认动作推进）
     * @return 包含最新 summary 与新增回合数；异常时返回 apply_failed
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
                        System.currentTimeMillis());
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

            Map<String, Object> updatedState = battleEngine.playRound(existingState,
                    playerMoveMap == null ? Map.of() : playerMoveMap);
            persistNewRounds((int) (long) battleId, existingState, updatedState);

            Integer opponentTeamId = row.get("opponent_team_id") == null ? null
                    : ((Number) row.get("opponent_team_id")).intValue();
            String playerTeamJson = toJson(updatedState.get("playerTeam"));
            String phase = String
                    .valueOf(updatedState.getOrDefault("phase", updatedState.getOrDefault("status", "battle")));
            battleMapper.updateBattleState(battleId.intValue(), opponentTeamId, toJson(updatedState),
                    roundsCount(updatedState), phase);
            battleMapper.updateBattleTeamState(battleId.intValue(), playerTeamJson, toJson(updatedState), phase);

            if ("completed".equals(updatedState.get("status"))) {
                finalizePlayerProgress(row, updatedState);
                Integer winnerPlayerId = "player".equals(updatedState.get("winner")) && row.get("player_id") != null
                        ? ((Number) row.get("player_id")).intValue()
                        : null;
                battleMapper.updateBattle(battleId.intValue(), opponentTeamId, toJson(updatedState),
                        roundsCount(updatedState), winnerPlayerId,
                        String.valueOf(updatedState.getOrDefault("phase", "completed")));
                if ("player".equals(updatedState.get("winner"))) {
                    Map<String, Object> latestTeam = teamMapper
                            .findLatestByPlayer(((Number) row.get("player_id")).intValue());
                    if (latestTeam != null && latestTeam.get("id") != null) {
                        poolService.addTeamToPool(((Number) latestTeam.get("id")).intValue(),
                                toInt(extractFactory(updatedState).get("playerRank"), 0));
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
            log.error("推进对战失败, battleId={}", battleId, e);
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
            Integer opponentTeamId = row.get("opponent_team_id") == null ? null
                    : ((Number) row.get("opponent_team_id")).intValue();
            String playerTeamJson = toJson(updatedState.get("playerTeam"));
            String phase = String.valueOf(updatedState.getOrDefault("phase", "battle"));

            battleMapper.updateBattleState(battleId.intValue(), opponentTeamId, toJson(updatedState),
                    roundsCount(updatedState), phase);
            battleMapper.updateBattleTeamState(battleId.intValue(), playerTeamJson, toJson(updatedState), phase);

            return Map.of(
                    "battleId", battleId,
                    "status", updatedState.get("status"),
                    "summary", updatedState,
                    "message", "替补上场已确认。");
        } catch (IllegalArgumentException e) {
            log.warn("替补选择无效, battleId={}", battleId, e);
            return Map.of("error", e.getMessage(), "message", "替补选择无效。");
        } catch (Exception e) {
            log.error("确认替补失败, battleId={}", battleId, e);
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
     * 玩家认输，直接结束当前对战。
     */
    public Map<String, Object> forfeitBattle(Long battleId, String username) {
        try {
            Map<String, Object> row = battleMapper.findBattleWithOpponent(battleId);
            if (row == null || row.isEmpty()) {
                return Map.of("error", "not_found");
            }

            Map<String, Object> existingState = parseSummaryState(row.get("summary_json"));
            if ("completed".equals(existingState.get("status"))) {
                return Map.of("error", "already_completed", "summary", existingState);
            }

            existingState.put("status", "completed");
            existingState.put("phase", "completed");
            existingState.put("winner", "opponent");
            existingState.put("forfeit", true);

            Integer opponentTeamId = row.get("opponent_team_id") == null ? null
                    : ((Number) row.get("opponent_team_id")).intValue();
            battleMapper.updateBattle(battleId.intValue(), opponentTeamId, toJson(existingState),
                    roundsCount(existingState), null, "completed");
            finalizePlayerProgress(row, existingState);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "completed");
            response.put("winner", "opponent");
            response.put("forfeit", true);
            response.put("summary", existingState);
            response.put("message", "你选择了认输。");
            return response;
        } catch (Exception e) {
            log.error("认输失败, battleId={}, username={}", battleId, username, e);
            return Map.of("error", "forfeit_failed", "detail", e.getMessage());
        }
    }

    /**
     * 确认队伍预览阶段选择，并正式进入 battle 阶段。
     *
     * @param battleId  对战主键
     * @param selection 预览选择，通常包含 pickedRosterIndexes 与 leadRosterIndexes
     * @return 返回更新后的 summary；若阶段不匹配则返回 preview_closed
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
            Integer opponentTeamId = row.get("opponent_team_id") == null ? null
                    : ((Number) row.get("opponent_team_id")).intValue();
            String playerTeamJson = toJson(updatedState.get("playerTeam"));
            String phase = String.valueOf(updatedState.getOrDefault("phase", "battle"));

            battleMapper.updateBattleState(battleId.intValue(), opponentTeamId, toJson(updatedState), 0, phase);
            battleMapper.updateBattleTeamState(battleId.intValue(), playerTeamJson, toJson(updatedState), phase);

            return Map.of(
                    "battleId", battleId,
                    "status", updatedState.get("status"),
                    "summary", updatedState,
                    "message", "队伍预览已确认，对战开始。");
        } catch (Exception e) {
            log.error("确认队伍预览失败, battleId={}", battleId, e);
            return Map.of("error", "preview_confirm_failed", "detail", e.getMessage());
        }
    }

    /**
     * 处理胜利后的交换奖励逻辑，并把变更后的队伍重新写回玩家当前队伍。
     * <p>
     * 前置条件：当前 battle 必须是玩家胜利且 exchangeAvailable=true。
     * 该方法会写入 exchange 记录、执行队伍乐观锁更新，并把 battle summary 同步为新队伍。
     * </p>
     *
     * @param battleId       对战主键
     * @param replacedIndex  玩家队伍中被替换成员索引
     * @param newPokemonJson 新成员 JSON（单体）
     * @return 成功返回最新 summary 与被替换成员；失败返回 exchange_failed/具体错误
     */
    public Map<String, Object> updateBattleAfterExchange(Long battleId, int replacedIndex, String newPokemonJson) {
        try {
            Map<String, Object> battleStatus = getBattleStatus(battleId);
            if (battleStatus.containsKey("error")) {
                return battleStatus;
            }

            Map<String, Object> battle = castMap(battleStatus.get("battle"));
            Map<String, Object> state = castMap(battleStatus.get("summary"));
            if (!"completed".equals(state.get("status")) || !"player".equals(state.get("winner"))
                    || !Boolean.TRUE.equals(state.get("exchangeAvailable"))) {
                return Map.of("error", "exchange_not_available");
            }

            Map<String, Object> latestTeam = teamMapper
                    .findLatestByPlayer(((Number) battle.get("player_id")).intValue());
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

            // 持久化交换记录
            try {
                exchangeMapper.insertExchange(battleId, playerTeamId, null, replacedIndex, toJson(replaced),
                        newPokemonJson);
            } catch (Exception e) {
                log.warn("写入交换记录失败, battleId={}, playerTeamId={}", battleId, playerTeamId, e);
            }

            if (teamMapper.updateTeamWithVersion(playerTeamId, updatedTeamJson, version) == 0) {
                return Map.of("error", "team_stale");
            }

            Map<String, Object> updatedState = battleEngine.replacePlayerTeamMember(state, replacedIndex, replacement);
            battleMapper.updateBattleTeamState(battleId.intValue(), updatedTeamJson, toJson(updatedState), String
                    .valueOf(updatedState.getOrDefault("phase", updatedState.getOrDefault("status", "completed"))));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "ok");
            response.put("summary", updatedState);
            response.put("replacedPokemon", replaced);
            return response;
        } catch (Exception e) {
            log.error("交换奖励处理失败, battleId={}, replacedIndex={}", battleId, replacedIndex, e);
            return Map.of("error", "exchange_failed", "detail", e.getMessage());
        }
    }

    /**
     * 把对战工厂模式下需要展示的额外元信息写进 summary.factory。
     */
    private void enrichStateMetadata(Map<String, Object> state, String mode, String username, int playerRank,
            String opponentSource) {
        Map<String, Object> factory = extractFactory(state);
        factory.put("mode", mode);
        factory.put("username", username);
        factory.put("playerRank", playerRank);
        factory.put("opponentSource", opponentSource);
        factory.put("teamSize", FACTORY_TEAM_SIZE);
    }

    /**
     * 对战结束后结算玩家积分和段位。
     * <p>
     * 幂等保护：factory.progressApplied=true 时直接返回，避免重复结算。
     * 工厂挑战模式下只更新胜负统计，积分由 FactoryRunService 统一结算；
     * 非工厂模式下按单场规则更新 rank/points 与 tier 体系。
     * </p>
     *
     * @param battleRow battle 表行数据（至少包含 player_id）
     * @param state     当前对战状态（会写入 factory 结算元信息）
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

        Map<String, Object> playerProfile = playerMapper
                .findByUsername(String.valueOf(factory.getOrDefault("username", "")));
        if (playerProfile == null || playerProfile.isEmpty()) {
            return;
        }

        int points = toInt(playerProfile.get("points"), 0);
        int tier = toInt(playerProfile.get("tier"), 0);
        int tierPoints = toInt(playerProfile.get("tier_points"), 0);
        int totalPoints = toInt(playerProfile.get("total_points"), 0);
        int highestTier = toInt(playerProfile.get("highest_tier"), 0);
        int totalWins = toInt(playerProfile.get("wins"), 0);
        int totalLosses = toInt(playerProfile.get("losses"), 0);
        boolean won = "player".equals(state.get("winner"));

        // 工厂挑战模式下，积分由 FactoryRunService 统一结算
        Integer factoryRunId = factory.get("factoryRunId") instanceof Number n ? n.intValue() : null;
        if (factoryRunId != null) {
            factory.put("progressApplied", true);
            factory.put("playerTier", tier);
            factory.put("playerTierName", TierService.tierName(tier));
            // 更新胜负统计
            playerMapper.updateTierProgress(playerId, tier, tierPoints, totalPoints, highestTier,
                    totalWins + (won ? 1 : 0), totalLosses + (won ? 0 : 1));
            // 通知工厂挑战服务更新进度
            String updatedTeamJson = toJson(state.get("playerTeam"));
            factoryRunService.onBattleCompleted(factoryRunId, won, updatedTeamJson);
            return;
        }

        // 非工厂模式：单场积分结算
        int delta = TierService.calculateSingleBattleReward(tier, won);
        Map<String, Object> tierResult = TierService.applyPoints(tier, tierPoints, totalPoints, delta);
        int newTier = (int) tierResult.get("tier");
        int newTierPoints = (int) tierResult.get("tierPoints");
        int newTotalPoints = (int) tierResult.get("totalPoints");
        int newHighestTier = Math.max(highestTier, newTier);

        // 兼容旧 rank/points 字段
        points = won ? points + 10 : Math.max(0, points - 3);
        int rank = points / 30;
        playerMapper.updateProgress(playerId, rank, points);
        playerMapper.updateTierProgress(playerId, newTier, newTierPoints, newTotalPoints, newHighestTier,
                totalWins + (won ? 1 : 0), totalLosses + (won ? 0 : 1));

        factory.put("progressApplied", true);
        factory.put("playerRank", rank);
        factory.put("playerPoints", points);
        factory.put("playerTier", newTier);
        factory.put("playerTierName", TierService.tierName(newTier));
        factory.put("playerTierPoints", newTierPoints);
        factory.put("playerTotalPoints", newTotalPoints);
        factory.put("pointsDelta", delta);
        factory.put("promoted", tierResult.get("promoted"));
        factory.put("demoted", tierResult.get("demoted"));
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
            } catch (Exception e) {
                throw new IllegalStateException("battle_round_persist_failed", e);
            }
        }
    }

    /**
     * 解析玩家当前应使用的队伍。
     * <p>
     * 优先级依次为：请求自带队伍 > 玩家最近一次队伍 > 系统自动生成队伍。
     * 该方法带有写操作：命中 custom/generated 分支时会向 team 表插入新记录。
     * </p>
     *
     * @param playerId          玩家主键
     * @param playerRank        当前段位/等级
     * @param seed              随机种子
     * @param requestedTeamJson 前端显式传入的队伍 JSON
     * @return 包含 teamId/teamJson/source 的结构
     */
    private Map<String, Object> resolvePlayerTeam(Integer playerId, int playerRank, long seed,
            String requestedTeamJson) {
        if (!aiService.isBlankTeamJson(requestedTeamJson)) {
            teamMapper.insertTeam(playerId, "custom-" + seed, requestedTeamJson, "player");
            return Map.of("teamId", teamMapper.lastInsertId(), "teamJson", requestedTeamJson, "source", "custom");
        }

        Map<String, Object> existing = teamMapper.findLatestByPlayer(playerId);
        if (existing != null && existing.get("team_json") != null
                && !aiService.isBlankTeamJson(String.valueOf(existing.get("team_json")))) {
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
     * 对手池抽样窗口与样本量来自配置，便于按环境调优匹配性能与公平性。
     * </p>
     *
     * @param playerRank    玩家段位，用于控制匹配强度
     * @param seed          随机种子
     * @param excludedNames 需要规避的宝可梦名集合（通常为玩家队伍名集）
     * @return 包含 teamId/teamJson/source 的结构
     */
    private Map<String, Object> resolveOpponentTeam(int playerRank, long seed, Set<String> excludedNames) {
        // 使用配置化的窗口大小
        int window = config.getMatchmaking().getPoolWindow();
        int sampleSize = config.getMatchmaking().getPoolSampleSize();

        List<Map<String, Object>> poolCandidates = poolService.sample(playerRank, window, sampleSize);
        if (poolCandidates != null && !poolCandidates.isEmpty()) {
            for (Map<String, Object> candidate : poolCandidates) {
                String teamJson = String.valueOf(candidate.get("team_json"));
                Set<String> candidateNames = aiService.extractNames(teamJson);

                // 检查是否有重复的宝可梦
                if (!candidateNames.equals(excludedNames)) {
                    return Map.of(
                            "teamId", candidate.get("team_id"),
                            "teamJson", teamJson,
                            "source", "pool");
                }
            }
        }

        // 如果对手池没有合适的，使用AI生成
        // 根据配置决定对手强度加成
        int opponentRank = Math.max(0, playerRank + config.getMatchmaking().getAiOpponentRankBonus());
        String generatedTeamJson = aiService.generateFactoryTeamJson(FACTORY_TEAM_SIZE, opponentRank, seed,
                excludedNames);
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

    /**
     * 解析玩家动作映射，兼容 Map 与 JSON 字符串两种输入。
     *
     * @param rawValue 控制层透传的原始值
     * @return 标准化后的键值映射；无输入时返回空 Map
     * @throws IllegalArgumentException 当 JSON 解析失败时抛出
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> parsePlayerMoveMap(Object rawValue) {
        if (rawValue instanceof Map<?, ?> rawMap) {
            Map<String, String> moveMap = new LinkedHashMap<>();
            rawMap.forEach(
                    (key, value) -> moveMap.put(String.valueOf(key), value == null ? "" : String.valueOf(value)));
            return moveMap;
        }
        if (rawValue instanceof String rawString && !rawString.isBlank()) {
            try {
                return objectMapper.readValue(rawString, Map.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid_move_map_json", e);
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
            throw new IllegalStateException("serialize_failed", e);
        }
    }
}
