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

    private final JdbcTemplate jdbcTemplate;
    private final BattleEngine battleEngine;
    private final OpponentPoolService poolService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BattleService(JdbcTemplate jdbcTemplate, BattleEngine battleEngine, OpponentPoolService poolService) {
        this.jdbcTemplate = jdbcTemplate;
        this.battleEngine = battleEngine;
        this.poolService = poolService;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> startMatch(Map<String, Object> req) {
        Map<String, Object> out = new HashMap<>();
        String username = (String) req.getOrDefault("username", "guest");
        String teamJson = (String) req.get("teamJson"); // optional

        // upsert player
        jdbcTemplate.update("INSERT OR IGNORE INTO player(username, rank, points) VALUES(?, 0, 0)", username);
        Integer playerId = jdbcTemplate.queryForObject("SELECT id FROM player WHERE username = ?", Integer.class, username);

        // ensure player team exists
        Integer playerTeamId = null;
        if (teamJson != null && !teamJson.isEmpty()) {
            jdbcTemplate.update("INSERT INTO team(player_id, name, team_json, source, created_at) VALUES (?, ?, ?, 'player', datetime('now'))", playerId, "custom-" + System.currentTimeMillis(), teamJson);
            playerTeamId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
        } else {
            // attempt to find an existing team for player
            List<Map<String,Object>> teams = jdbcTemplate.queryForList("SELECT id, team_json FROM team WHERE player_id = ? LIMIT 1", playerId);
            if (!teams.isEmpty()) {
                playerTeamId = (Integer) teams.get(0).get("id");
                teamJson = (String) teams.get(0).get("team_json");
            } else {
                // generate a simple random team based on available pokemon table
                List<Map<String,Object>> sample = jdbcTemplate.queryForList("SELECT id, name, base_experience FROM pokemon LIMIT 6");
                try {
                    teamJson = objectMapper.writeValueAsString(sample);
                    jdbcTemplate.update("INSERT INTO team(player_id, name, team_json, source, created_at) VALUES (?, ?, ?, 'generated', datetime('now'))", playerId, "generated-" + System.currentTimeMillis(), teamJson);
                    playerTeamId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
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
            List<Map<String,Object>> sample = jdbcTemplate.queryForList("SELECT id, name, base_experience FROM pokemon ORDER BY RANDOM() LIMIT 6");
            try {
                opponentTeamJson = objectMapper.writeValueAsString(sample);
                jdbcTemplate.update("INSERT INTO team(player_id, name, team_json, source, created_at) VALUES (?, ?, ?, 'generated', datetime('now'))", null, "opp-generated-" + System.currentTimeMillis(), opponentTeamJson);
                opponentTeamId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
            } catch (Exception e) {
                opponentTeamJson = "[]";
            }
        }

        // simulate battle
        Map<String,Object> summary = battleEngine.simulate(teamJson, opponentTeamJson, 10);

        // persist battle
        try {
            String summaryJson = objectMapper.writeValueAsString(summary);
            jdbcTemplate.update("INSERT INTO battle(player_id, opponent_team_id, rounds, summary_json, started_at, ended_at, winner_player_id) VALUES (?, ?, ?, ?, datetime('now'), datetime('now'), ?)",
                    playerId, opponentTeamId, summary.getOrDefault("roundsCount", 0), summaryJson,
                    "player".equals(summary.get("winner")) ? playerId : null);
            Integer battleId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
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
        List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT b.id, b.player_id, b.opponent_team_id, b.started_at, b.ended_at, b.summary_json, t.team_json AS opponent_team_json FROM battle b LEFT JOIN team t ON b.opponent_team_id = t.id WHERE b.id = ?", battleId);
        if (rows.isEmpty()) {
            out.put("error", "not found");
        } else {
            out.put("battle", rows.get(0));
        }
        return out;
    }

    public List<Map<String, Object>> samplePool(Integer rank) {
        if (rank == null) rank = 0;
        return poolService.sample(rank, 1, 5);
    }
}
