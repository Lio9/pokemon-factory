package com.lio9.battle.controller;

import com.lio9.battle.mapper.PlayerMapper;
import com.lio9.battle.mapper.TeamMapper;
import com.lio9.battle.mapper.BattleExchangeMapper;
import com.lio9.battle.service.BattleService;
import com.lio9.battle.service.BattleExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    @Autowired
    private BattleService battleService;

    @Autowired
    private BattleExecutor battleExecutor;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private BattleExchangeMapper exchangeMapper;

    @PostMapping("/start")
    public ResponseEntity<?> startBattle(@RequestBody Map<String, Object> req) {
        // username must come from authenticated principal
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        req.put("username", username);
        Map<String, Object> result = battleService.startMatch(req);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/start-async")
    public ResponseEntity<?> startAsync(@RequestBody Map<String, Object> req) {
        String username = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        playerMapper.insertIgnore(username);
        Integer playerId = playerMapper.findIdByUsername(username);
        String teamJson = (String) req.get("teamJson");
        String pmJson = null;
        try { pmJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req.get("playerMoveMap")); } catch (Exception ignore) {}
        Integer battleId = battleExecutor.submitAsyncBattle(playerId, teamJson == null ? "[]" : teamJson, pmJson);
        return ResponseEntity.ok(Map.of("battleId", battleId));
    }

    @GetMapping("/status/{battleId}")
    public ResponseEntity<?> status(@PathVariable Long battleId) {
        Map<String, Object> status = battleService.getBattleStatus(battleId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/pool")
    public ResponseEntity<?> pool(@RequestParam(required = false) Integer rank) {
        return ResponseEntity.ok(battleService.samplePool(rank));
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchange(@RequestBody Map<String, Object> req) {
        try {
            Number bidNum = (Number) req.get("battleId");
            if (bidNum == null) return ResponseEntity.badRequest().body(Map.of("error","missing_battleId"));
            Long battleId = bidNum.longValue();

            Number repNum = (Number) req.get("replacedIndex");
            int replacedIndex = repNum == null ? -1 : repNum.intValue();

            String newPokemonJson = (String) req.get("newPokemonJson");

            Map<String,Object> battle = battleService.getBattleStatus(battleId);
            if (battle.containsKey("error")) return ResponseEntity.badRequest().body(Map.of("error","battle_not_found"));
            Map<String,Object> b = (Map<String,Object>) battle.get("battle");

            Number pNum = (Number) b.get("player_id");
            if (pNum == null) return ResponseEntity.badRequest().body(Map.of("error","player_not_found"));
            Integer playerId = pNum.intValue();

            Map<String,Object> teamRow = teamMapper.findLatestByPlayer(playerId);
            if (teamRow == null || !teamRow.containsKey("id")) return ResponseEntity.badRequest().body(Map.of("error","team_not_found"));
            Integer playerTeamId = (Integer) teamRow.get("id");
            String teamJson = (String) teamRow.get("team_json");
            Integer version = teamRow.get("version") == null ? 0 : ((Number)teamRow.get("version")).intValue();

            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var arr = mapper.readValue(teamJson, java.util.List.class);
            Object replacedPokemon = null;
            if (replacedIndex >=0 && replacedIndex < arr.size()) {
                replacedPokemon = arr.set(replacedIndex, mapper.readValue(newPokemonJson, java.util.Map.class));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error","invalid_replaced_index"));
            }
            String updated = mapper.writeValueAsString(arr);

            int updatedRows = teamMapper.updateTeamWithVersion(playerTeamId, updated, version);
            if (updatedRows == 0) {
                return ResponseEntity.status(409).body(Map.of("error","team_stale","message","Team was updated concurrently. Please refresh and try again."));
            }

            Number oppNum = (Number) b.get("opponent_team_id");
            Integer opponentTeamId = oppNum == null ? null : oppNum.intValue();

            exchangeMapper.insertExchange(battleId, playerTeamId, opponentTeamId, replacedIndex, mapper.writeValueAsString(replacedPokemon), newPokemonJson);
            return ResponseEntity.ok(Map.of("status","ok"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "exchange_failed", "detail", e.getMessage()));
        }
    }
}
