package com.lio9.battle.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BattleService {

    private final JdbcTemplate jdbcTemplate;

    public BattleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> startMatch(Map<String, Object> req) {
        Map<String, Object> out = new HashMap<>();
        String username = (String) req.getOrDefault("username", "guest");
        // upsert player
        jdbcTemplate.update("INSERT OR IGNORE INTO player(username, rank, points) VALUES(?, 0, 0)", username);
        Integer playerId = jdbcTemplate.queryForObject("SELECT id FROM player WHERE username = ?", Integer.class, username);
        // choose opponent: simple random from opponent_pool
        List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT team_id, rank FROM opponent_pool ORDER BY RANDOM() LIMIT 1");
        Integer opponentTeamId = null;
        String source = "generated";
        if (!rows.isEmpty()) {
            opponentTeamId = (Integer) rows.get(0).get("team_id");
            source = "pool";
        }
        // create battle record
        jdbcTemplate.update("INSERT INTO battle(player_id, opponent_team_id, rounds) VALUES (?, ?, 0)", playerId, opponentTeamId);
        Integer battleId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
        out.put("battleId", battleId);
        out.put("playerId", playerId);
        out.put("opponentTeamId", opponentTeamId);
        out.put("source", source);
        out.put("message", "Match created (skeleton). Implement game loop later.");
        return out;
    }

    public Map<String, Object> getBattleStatus(Long battleId) {
        Map<String, Object> out = new HashMap<>();
        List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT id, player_id, opponent_team_id, started_at, ended_at FROM battle WHERE id = ?", battleId);
        if (rows.isEmpty()) {
            out.put("error", "not found");
        } else {
            out.put("battle", rows.get(0));
        }
        return out;
    }

    public List<Map<String, Object>> samplePool(Integer rank) {
        if (rank == null) rank = 0;
        return jdbcTemplate.queryForList("SELECT op.id, t.team_json, op.rank FROM opponent_pool op JOIN team t ON op.team_id = t.id WHERE op.rank BETWEEN ? AND ? ORDER BY RANDOM() LIMIT 5", Math.max(rank-1,0), rank+1);
    }
}
