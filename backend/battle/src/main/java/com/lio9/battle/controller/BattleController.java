package com.lio9.battle.controller;

import com.lio9.battle.config.BattleApiResponseSupport;
import com.lio9.battle.service.BattleExecutor;
import com.lio9.battle.service.BattleService;
import com.lio9.battle.service.FactoryRunService;
import com.lio9.battle.mapper.PlayerMapper;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/battle")
public class BattleController {
    private final BattleService battleService;
    private final BattleExecutor battleExecutor;
    private final PlayerMapper playerMapper;
    private final FactoryRunService factoryRunService;

    public BattleController(BattleService battleService, BattleExecutor battleExecutor,
                            PlayerMapper playerMapper, FactoryRunService factoryRunService) {
        this.battleService = battleService;
        this.battleExecutor = battleExecutor;
        this.playerMapper = playerMapper;
        this.factoryRunService = factoryRunService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startBattle(@RequestBody Map<String, Object> req, Authentication authentication) {
        req.put("username", authenticatedUsername(authentication));
        return BattleApiResponseSupport.fromPayload(battleService.startMatch(req));
    }

    @PostMapping("/start-async")
    public ResponseEntity<?> startAsync(@RequestBody Map<String, Object> req, Authentication authentication) {
        String username = authenticatedUsername(authentication);
        playerMapper.insertIgnore(username);
        Integer playerId = playerMapper.findIdByUsername(username);
        String teamJson = req.get("teamJson") instanceof String ? req.get("teamJson").toString() : null;
        String moveJson = toMoveJson(req.get("playerMoveMap"));
        Integer battleId = battleExecutor.submitAsyncBattle(playerId, teamJson, moveJson);
        if (battleId == null) {
            return BattleApiResponseSupport.error(HttpStatus.INTERNAL_SERVER_ERROR, "submit_failed", "异步对战提交失败。");
        }
        return BattleApiResponseSupport.success(Map.of("battleId", battleId));
    }

    @GetMapping("/status/{battleId}")
    public ResponseEntity<?> status(@PathVariable Long battleId) {
        return BattleApiResponseSupport.fromPayload(battleService.getBattleStatus(battleId));
    }

    @GetMapping("/pool")
    public ResponseEntity<?> pool(@RequestParam(required = false) Integer rank) {
        return BattleApiResponseSupport.success(battleService.samplePool(rank));
    }

    @PostMapping("/{battleId}/preview")
    public ResponseEntity<?> confirmPreview(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return BattleApiResponseSupport.fromPayload(battleService.confirmTeamPreview(battleId, req));
    }

    @PostMapping("/{battleId}/replacement")
    public ResponseEntity<?> confirmReplacement(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return BattleApiResponseSupport.fromPayload(battleService.confirmReplacement(battleId, req));
    }

    @PostMapping("/{battleId}/move")
    public ResponseEntity<?> move(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return BattleApiResponseSupport.fromPayload(battleService.applyMove(battleId, normalizeMoveMap(req)));
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchange(@RequestBody Map<String, Object> req) {
        Number battleId = (Number) req.get("battleId");
        Number replacedIndex = (Number) req.get("replacedIndex");
        String newPokemonJson = req.get("newPokemonJson") == null ? null : req.get("newPokemonJson").toString();
        if (battleId == null || replacedIndex == null || newPokemonJson == null || newPokemonJson.isBlank()) {
            return BattleApiResponseSupport.error(HttpStatus.BAD_REQUEST, "missing_fields", "请求缺少必要字段。");
        }
        return BattleApiResponseSupport.fromPayload(
                battleService.updateBattleAfterExchange(battleId.longValue(), replacedIndex.intValue(), newPokemonJson));
    }

    @PostMapping("/{battleId}/forfeit")
    public ResponseEntity<?> forfeit(@PathVariable Long battleId, Authentication authentication) {
        return BattleApiResponseSupport.fromPayload(
                battleService.forfeitBattle(battleId, authenticatedUsername(authentication)));
    }

    @PostMapping("/factory/start")
    public ResponseEntity<?> startFactoryRun(Authentication authentication) {
        return BattleApiResponseSupport.fromPayload(factoryRunService.startRun(authenticatedUsername(authentication)));
    }

    @PostMapping("/factory/{runId}/next")
    public ResponseEntity<?> nextFactoryBattle(@PathVariable Integer runId, Authentication authentication) {
        return BattleApiResponseSupport.fromPayload(
                factoryRunService.startNextBattle(authenticatedUsername(authentication), runId));
    }

    @PostMapping("/factory/abandon")
    public ResponseEntity<?> abandonFactoryRun(Authentication authentication) {
        return BattleApiResponseSupport.fromPayload(factoryRunService.abandonRun(authenticatedUsername(authentication)));
    }

    @GetMapping("/factory/status")
    public ResponseEntity<?> factoryRunStatus(Authentication authentication) {
        return BattleApiResponseSupport.fromPayload(factoryRunService.getRunStatus(authenticatedUsername(authentication)));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(Authentication authentication) {
        return BattleApiResponseSupport.success(factoryRunService.getProfile(authenticatedUsername(authentication)));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> leaderboard(@RequestParam(defaultValue = "50") int limit) {
        return BattleApiResponseSupport.success(factoryRunService.getLeaderboard(limit));
    }

    /** 从请求中提取 playerMoveMap，委托给 Service 的统一解析方法 */
    private Map<String, String> normalizeMoveMap(Map<String, Object> req) {
        if (req == null) return Map.of();
        Object moveMap = req.get("playerMoveMap");
        if (moveMap != null) return battleService.parsePlayerMoveMap(moveMap);
        if (req.get("move") != null) return Map.of("__active", String.valueOf(req.get("move")));
        return Map.of();
    }

    private String toMoveJson(Object rawMoveMap) {
        try {
            return rawMoveMap == null ? null : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(rawMoveMap);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid_move_map", e);
        }
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            throw new IllegalStateException("authenticated user missing");
        }
        return authentication.getName();
    }
}
