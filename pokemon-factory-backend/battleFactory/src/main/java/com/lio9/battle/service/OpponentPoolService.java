package com.lio9.battle.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class OpponentPoolService {
    private final JdbcTemplate jdbcTemplate;

    public OpponentPoolService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addTeamToPool(Integer teamId, Integer rank) {
        if (teamId == null) return;
        jdbcTemplate.update("INSERT INTO opponent_pool(team_id, rank, created_at) VALUES (?, ?, datetime('now'))", teamId, rank == null ? 0 : rank);
    }

    public List<Map<String, Object>> sample(int rank, int window, int limit) {
        int low = Math.max(0, rank - window);
        int high = rank + window;
        return jdbcTemplate.queryForList("SELECT op.id, t.team_json, op.rank FROM opponent_pool op JOIN team t ON op.team_id = t.id WHERE op.rank BETWEEN ? AND ? ORDER BY RANDOM() LIMIT ?", low, high, limit);
    }

    public void cleanupOlderThanDays(int days) {
        jdbcTemplate.update("DELETE FROM opponent_pool WHERE created_at < datetime('now', ?)", "-" + days + " days");
    }
}
