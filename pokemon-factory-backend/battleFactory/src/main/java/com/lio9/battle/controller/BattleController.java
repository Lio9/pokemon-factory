package com.lio9.battle.controller;

import com.lio9.battle.service.BattleService;
import com.lio9.battle.service.BattleExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/start")
    public ResponseEntity<?> startBattle(@RequestBody Map<String, Object> req) {
        Map<String, Object> result = battleService.startMatch(req);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/start-async")
    public ResponseEntity<?> startAsync(@RequestBody Map<String, Object> req) {
        String username = (String) req.getOrDefault("username", "guest");
        jdbcTemplate.update("INSERT OR IGNORE INTO player(username, rank, points) VALUES(?, 0, 0)", username);
        Integer playerId = jdbcTemplate.queryForObject("SELECT id FROM player WHERE username = ?", Integer.class, username);
        String teamJson = (String) req.get("teamJson");
        Integer battleId = battleExecutor.submitAsyncBattle(playerId, teamJson == null ? "[]" : teamJson);
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
            Integer battleId = (Integer) req.get("battleId");
            Integer replacedIndex = (Integer) req.get("replacedIndex");
            String newPokemonJson = (String) req.get("newPokemonJson");
            Map<String,Object> battle = battleService.getBattleStatus(battleId);
            if (battle.containsKey("error")) return ResponseEntity.badRequest().body(Map.of("error","battle_not_found"));
            Map<String,Object> b = (Map<String,Object>) battle.get("battle");
            Integer playerId = (Integer) b.get("player_id");
            Integer playerTeamId = jdbcTemplate.queryForObject("SELECT id FROM team WHERE player_id = ? ORDER BY created_at DESC LIMIT 1", Integer.class, playerId);
            String teamJson = jdbcTemplate.queryForObject("SELECT team_json FROM team WHERE id = ?", String.class, playerTeamId);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var arr = mapper.readValue(teamJson, java.util.List.class);
            Object replacedPokemon = null;
            if (replacedIndex >=0 && replacedIndex < arr.size()) {
                replacedPokemon = arr.set(replacedIndex, mapper.readValue(newPokemonJson, java.util.Map.class));
            }
            String updated = mapper.writeValueAsString(arr);
            jdbcTemplate.update("UPDATE team SET team_json = ? WHERE id = ?", updated, playerTeamId);
            jdbcTemplate.update("INSERT INTO battle_exchange(battle_id, player_team_id, opponent_team_id, replaced_index, replaced_pokemon_json, new_pokemon_json) VALUES (?, ?, ?, ?, ?, ?)",
                    battleId, playerTeamId, b.get("opponent_team_id"), replacedIndex, mapper.writeValueAsString(replacedPokemon), newPokemonJson);
            return ResponseEntity.ok(Map.of("status","ok"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "exchange_failed", "detail", e.getMessage()));
        }
    }
}
