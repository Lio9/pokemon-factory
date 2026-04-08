package com.lio9.battle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.service.BattleExecutor;
import com.lio9.battle.service.BattleService;
import com.lio9.battle.mapper.PlayerMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/battle")
public class BattleController {
    private final BattleService battleService;
    private final BattleExecutor battleExecutor;
    private final PlayerMapper playerMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BattleController(BattleService battleService, BattleExecutor battleExecutor, PlayerMapper playerMapper) {
        this.battleService = battleService;
        this.battleExecutor = battleExecutor;
        this.playerMapper = playerMapper;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startBattle(@RequestBody Map<String, Object> req) {
        req.put("username", authenticatedUsername());
        return ResponseEntity.ok(battleService.startMatch(req));
    }

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

    @GetMapping("/status/{battleId}")
    public ResponseEntity<?> status(@PathVariable Long battleId) {
        return ResponseEntity.ok(battleService.getBattleStatus(battleId));
    }

    @GetMapping("/pool")
    public ResponseEntity<?> pool(@RequestParam(required = false) Integer rank) {
        return ResponseEntity.ok(battleService.samplePool(rank));
    }

    @PostMapping("/{battleId}/preview")
    public ResponseEntity<?> confirmPreview(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(battleService.confirmTeamPreview(battleId, req));
    }

    @PostMapping("/{battleId}/replacement")
    public ResponseEntity<?> confirmReplacement(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(battleService.confirmReplacement(battleId, req));
    }

    @PostMapping("/{battleId}/move")
    public ResponseEntity<?> move(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(battleService.applyMove(battleId, normalizeMoveMap(req)));
    }

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

    @SuppressWarnings("unchecked")
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

    private String toMoveJson(Object rawMoveMap) {
        try {
            return rawMoveMap == null ? null : objectMapper.writeValueAsString(rawMoveMap);
        } catch (Exception e) {
            return null;
        }
    }

    private String authenticatedUsername() {
        return String.valueOf(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
