package com.lio9.battle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.service.BattleExecutor;
import com.lio9.battle.service.BattleService;
import com.lio9.battle.service.FactoryRunService;
import com.lio9.battle.mapper.PlayerMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对战工厂 HTTP 接口入口。
 * <p>
 * 该控制器只负责请求参数整理、认证用户名注入和响应透传，
 * 真正的对战编排、状态推进和持久化都下沉到 BattleService / BattleExecutor。
 * </p>
 */
@RestController
@RequestMapping("/api/battle")
public class BattleController {
    private final BattleService battleService;
    private final BattleExecutor battleExecutor;
    private final PlayerMapper playerMapper;
    private final FactoryRunService factoryRunService;
    private final ObjectMapper objectMapper;

    public BattleController(BattleService battleService, BattleExecutor battleExecutor, PlayerMapper playerMapper, FactoryRunService factoryRunService, ObjectMapper objectMapper) {
        this.battleService = battleService;
        this.battleExecutor = battleExecutor;
        this.playerMapper = playerMapper;
        this.factoryRunService = factoryRunService;
        this.objectMapper = objectMapper;
    }

    /**
     * 开始一场手动对战。
     * <p>
     * 当前登录用户名统一从安全上下文中读取，不允许前端伪造其他用户名。
     * </p>
     */
    @PostMapping("/start")
    public ResponseEntity<?> startBattle(@RequestBody Map<String, Object> req) {
        req.put("username", authenticatedUsername());
        return ResponseEntity.ok(battleService.startMatch(req));
    }

    /**
     * 提交一场异步自动模拟任务。
     */
    @PostMapping("/start-async")
    public ResponseEntity<?> startAsync(@RequestBody Map<String, Object> req) {
        String username = authenticatedUsername();
        playerMapper.insertIgnore(username);
        Integer playerId = playerMapper.findIdByUsername(username);
        String teamJson = req.get("teamJson") instanceof String ? req.get("teamJson").toString() : null;
        String moveJson = toMoveJson(req.get("playerMoveMap"));
        Integer battleId = battleExecutor.submitAsyncBattle(playerId, teamJson, moveJson);
        return ResponseEntity.ok(Map.of("battleId", battleId));
    }

    /**
     * 查询当前对战状态与摘要。
     */
    @GetMapping("/status/{battleId}")
    public ResponseEntity<?> status(@PathVariable Long battleId) {
        return ResponseEntity.ok(battleService.getBattleStatus(battleId));
    }

    /**
     * 抽样查看当前段位附近的对手池。
     */
    @GetMapping("/pool")
    public ResponseEntity<?> pool(@RequestParam(required = false) Integer rank) {
        return ResponseEntity.ok(battleService.samplePool(rank));
    }

    /**
     * 确认队伍预览阶段的 6 选 4 与首发选择。
     */
    @PostMapping("/{battleId}/preview")
    public ResponseEntity<?> confirmPreview(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(battleService.confirmTeamPreview(battleId, req));
    }

    /**
     * 确认倒下补位选择。
     */
    @PostMapping("/{battleId}/replacement")
    public ResponseEntity<?> confirmReplacement(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(battleService.confirmReplacement(battleId, req));
    }

    /**
     * 提交当前回合动作。
     */
    @PostMapping("/{battleId}/move")
    public ResponseEntity<?> move(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(battleService.applyMove(battleId, normalizeMoveMap(req)));
    }

    /**
     * 胜利后执行交换奖励逻辑。
     */
    @PostMapping("/exchange")
    public ResponseEntity<?> exchange(@RequestBody Map<String, Object> req) {
        Number battleId = (Number) req.get("battleId");
        Number replacedIndex = (Number) req.get("replacedIndex");
        String newPokemonJson = req.get("newPokemonJson") == null ? null : req.get("newPokemonJson").toString();
        if (battleId == null || replacedIndex == null || newPokemonJson == null || newPokemonJson.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_fields"));
        }
        return ResponseEntity.ok(battleService.updateBattleAfterExchange(battleId.longValue(), replacedIndex.intValue(), newPokemonJson));
    }

    /**
     * 认输：主动放弃当前对战。
     */
    @PostMapping("/{battleId}/forfeit")
    public ResponseEntity<?> forfeit(@PathVariable Long battleId) {
        return ResponseEntity.ok(battleService.forfeitBattle(battleId, authenticatedUsername()));
    }

    // ========== 工厂挑战（9 轮连续对战）==========

    /**
     * 开始一次工厂挑战（9 轮连续对战）。
     */
    @PostMapping("/factory/start")
    public ResponseEntity<?> startFactoryRun() {
        return ResponseEntity.ok(factoryRunService.startRun(authenticatedUsername()));
    }

    /**
     * 在工厂挑战中开始下一场对战。
     */
    @PostMapping("/factory/{runId}/next")
    public ResponseEntity<?> nextFactoryBattle(@PathVariable Integer runId) {
        return ResponseEntity.ok(factoryRunService.startNextBattle(authenticatedUsername(), runId));
    }

    /**
     * 放弃当前工厂挑战并结算。
     */
    @PostMapping("/factory/abandon")
    public ResponseEntity<?> abandonFactoryRun() {
        return ResponseEntity.ok(factoryRunService.abandonRun(authenticatedUsername()));
    }

    /**
     * 获取当前工厂挑战状态。
     */
    @GetMapping("/factory/status")
    public ResponseEntity<?> factoryRunStatus() {
        return ResponseEntity.ok(factoryRunService.getRunStatus(authenticatedUsername()));
    }

    // ========== 玩家信息 ==========

    /**
     * 获取玩家完整信息（段位、积分、历史战绩）。
     */
    @GetMapping("/profile")
    public ResponseEntity<?> profile() {
        return ResponseEntity.ok(factoryRunService.getProfile(authenticatedUsername()));
    }

    /**
     * 排行榜（大师球段位）。
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<?> leaderboard(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(factoryRunService.getLeaderboard(limit));
    }

    @SuppressWarnings("unchecked")
    /**
     * 兼容前端不同形态的出招提交结构，统一折叠成 battle service 能识别的扁平 Map。
     */
    private Map<String, String> normalizeMoveMap(Map<String, Object> req) {
        if (req == null) {
            return Map.of();
        }
        if (req.get("playerMoveMap") instanceof Map<?, ?> rawMap) {
            Map<String, String> moveMap = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> moveMap.put(String.valueOf(key), value == null ? "" : String.valueOf(value)));
            return moveMap;
        }
        if (req.get("playerMoveMap") instanceof String rawJson && !rawJson.isBlank()) {
            try {
                return objectMapper.readValue(rawJson, Map.class);
            } catch (Exception ignored) {
            }
        }
        if (req.get("move") != null) {
            return Map.of("__active", String.valueOf(req.get("move")));
        }
        return Map.of();
    }

    /**
     * 把任意原始动作对象转成 JSON，供异步任务直接落库。
     */
    private String toMoveJson(Object rawMoveMap) {
        try {
            return rawMoveMap == null ? null : objectMapper.writeValueAsString(rawMoveMap);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从安全上下文中提取当前认证用户名。
     */
    private String authenticatedUsername() {
        return String.valueOf(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
