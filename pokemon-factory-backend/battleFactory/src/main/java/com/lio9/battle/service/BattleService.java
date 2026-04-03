package com.lio9.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.engine.BattleEngine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BattleService {

    private final com.lio9.battle.mapper.PlayerMapper playerMapper;
    private final com.lio9.battle.mapper.TeamMapper teamMapper;
    private final com.lio9.battle.mapper.PokemonMapper pokemonMapper;
    private final com.lio9.battle.mapper.BattleMapper battleMapper;
    private final com.lio9.battle.mapper.BattleRoundMapper roundMapper;
    private final OpponentPoolService poolService;
    private final BattleEngine battleEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BattleService(com.lio9.battle.mapper.PlayerMapper playerMapper, com.lio9.battle.mapper.TeamMapper teamMapper, com.lio9.battle.mapper.PokemonMapper pokemonMapper, com.lio9.battle.mapper.BattleMapper battleMapper, com.lio9.battle.mapper.BattleRoundMapper roundMapper, BattleEngine battleEngine, OpponentPoolService poolService) {
        this.playerMapper = playerMapper;
        this.teamMapper = teamMapper;
        this.pokemonMapper = pokemonMapper;
        this.battleMapper = battleMapper;
        this.roundMapper = roundMapper;
        this.battleEngine = battleEngine;
        this.poolService = poolService;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> startMatch(Map<String, Object> req) {
        Map<String, Object> out = new HashMap<>();
        String username = (String) req.getOrDefault("username", "guest");
        String teamJson = (String) req.get("teamJson"); // optional

        // upsert player
        playerMapper.insertIgnore(username);
        Integer playerId = playerMapper.findIdByUsername(username);

        // ensure player team exists
        Integer playerTeamId = null;
        if (teamJson != null && !teamJson.isEmpty()) {
            teamMapper.insertTeam(playerId, "custom-" + System.currentTimeMillis(), teamJson, "player");
            playerTeamId = teamMapper.lastInsertId();
        } else {
            // attempt to find an existing team for player
            Map<String,Object> teamRow = teamMapper.findLatestByPlayer(playerId);
            if (teamRow != null && teamRow.get("id") != null) {
                playerTeamId = (Integer) teamRow.get("id");
                teamJson = (String) teamRow.get("team_json");
            } else {
                // generate a simple random team based on available pokemon table
                List<Map<String,Object>> sample = pokemonMapper.sampleLimit(6);
                try {
                    teamJson = objectMapper.writeValueAsString(sample);
                    teamMapper.insertTeam(playerId, "generated-" + System.currentTimeMillis(), teamJson, "generated");
                    playerTeamId = teamMapper.lastInsertId();
                } catch (Exception e) {
                    // fallback empty
                    teamJson = "[]";
                }
            }
        }

        // choose opponent
        List<Map<String,Object>> opponents = poolService.sample(0, 1, 1);
        Integer opponentTeamId = null;
        String source = "generated";
        String opponentTeamJson = "[]";
        if (!opponents.isEmpty()) {
            opponentTeamId = (Integer) opponents.get(0).get("id");
            opponentTeamJson = (String) opponents.get(0).get("team_json");
            source = "pool";
        } else {
            // generate a random opponent team
            List<Map<String,Object>> sample = pokemonMapper.sampleLimit(6);
            try {
                opponentTeamJson = objectMapper.writeValueAsString(sample);
                teamMapper.insertTeam(null, "opp-generated-" + System.currentTimeMillis(), opponentTeamJson, "generated");
                opponentTeamId = teamMapper.lastInsertId();
            } catch (Exception e) {
                opponentTeamJson = "[]";
            }
        }

        // parse playerMoveMap if provided
        Map<String, String> playerMoveMap = null;
        if (req.containsKey("playerMoveMap")) {
            try {
                playerMoveMap = objectMapper.readValue(req.get("playerMoveMap").toString(), Map.class);
            } catch (Exception e) {
                playerMoveMap = null;
            }
        }
        // simulate battle
        Map<String,Object> summary = battleEngine.simulate(teamJson, opponentTeamJson, 10, playerMoveMap);

        // persist battle
        try {
            String summaryJson = objectMapper.writeValueAsString(summary);
            Integer winnerId = "player".equals(summary.get("winner")) ? playerId : null;
            battleMapper.insertFinal(playerId, opponentTeamId, (Integer) summary.getOrDefault("roundsCount", 0), summaryJson, winnerId);
            Integer battleId = battleMapper.lastInsertId();
            out.put("battleId", battleId);
        } catch (Exception e) {
            out.put("error", "persist_failed");
        }

        // if player won, add to opponent pool
        if ("player".equals(summary.get("winner")) && playerTeamId != null) {
            poolService.addTeamToPool(playerTeamId, 0);
        }

        out.put("summary", summary);
        out.put("playerTeamJson", teamJson);
        out.put("opponentTeamJson", opponentTeamJson);
        out.put("source", source);
        out.put("message", "Battle simulated and stored.");
        return out;
    }

    public Map<String, Object> getBattleStatus(Long battleId) {
        Map<String, Object> out = new HashMap<>();
        Map<String,Object> row = battleMapper.findBattleWithOpponent(battleId);
        if (row == null || row.isEmpty()) {
            out.put("error", "not found");
        } else {
            out.put("battle", row);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public Map<String,Object> applyMove(Long battleId, Map<String,String> playerMoveMap) {
        Map<String,Object> out = new HashMap<>();
        try {
            Map<String,Object> row = battleMapper.findBattleWithOpponent(battleId);
            if (row == null) { out.put("error","not_found"); return out; }
            String playerTeamJson = (String) row.get("player_team_json");
            String opponentTeamJson = (String) row.get("opponent_team_json");
            if (playerTeamJson == null) playerTeamJson = "[]";
            if (opponentTeamJson == null) opponentTeamJson = "[]";

            // run one round simulation using provided move map
            Map<String,Object> summary = battleEngine.simulate(playerTeamJson, opponentTeamJson, 1, playerMoveMap);

            // persist round(s)
            var rounds = (java.util.List<Map<String,Object>>) summary.get("rounds");
            if (rounds != null) {
                for (Map<String,Object> r : rounds) {
                    try { roundMapper.insertRound(battleId.intValue(), (Integer)r.get("round"), objectMapper.writeValueAsString(r)); } catch (Exception ignore) {}
                }
            }

            // merge into existing summary_json
            String existingJson = row.get("summary_json") == null ? null : row.get("summary_json").toString();
            Map<String,Object> merged = new HashMap<>();
            java.util.List<Map<String,Object>> mergedRounds = new java.util.ArrayList<>();
            if (existingJson != null && !existingJson.isEmpty()) {
                try {
                    Object ex = objectMapper.readValue(existingJson, Object.class);
                    if (ex instanceof Map) {
                        Map<String,Object> exm = (Map<String,Object>) ex;
                        Object er = exm.get("rounds");
                        if (er instanceof java.util.List) mergedRounds.addAll((java.util.List<Map<String,Object>>)er);
                    }
                } catch (Exception ignore) {}
            }
            if (rounds != null) mergedRounds.addAll(rounds);
            merged.put("rounds", mergedRounds);
            merged.put("roundsCount", mergedRounds.size());
            // determine winner if decided
            if (summary.containsKey("winner")) merged.put("winner", summary.get("winner"));

            String mergedJson = objectMapper.writeValueAsString(merged);
            // update battle row
            Integer opponentTeamId = row.get("opponent_team_id") == null ? null : ((Number)row.get("opponent_team_id")).intValue();
            Integer winnerId = merged.get("winner") != null && "player".equals(merged.get("winner")) ? ((Number)row.get("player_id")).intValue() : null;
            battleMapper.updateBattle(battleId.intValue(), opponentTeamId, mergedJson, mergedRounds.size(), winnerId);

            out.put("summary", merged);
            out.put("status","ok");
            return out;
        } catch (Exception e) {
            out.put("error","apply_failed");
            out.put("detail", e.getMessage());
            return out;
        }
    }

    public List<Map<String, Object>> samplePool(Integer rank) {
        if (rank == null) rank = 0;
        return poolService.sample(rank, 1, 5);
    }
}
