package com.lio9.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.lio9.battle.engine.BattleEngine;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class BattleExecutor {
    private final JdbcTemplate jdbcTemplate;
    private final BattleEngine engine;
    private final OpponentPoolService poolService;
    private final ObjectMapper mapper = new ObjectMapper();
    private ExecutorService executor;

    public BattleExecutor(JdbcTemplate jdbcTemplate, BattleEngine engine, OpponentPoolService poolService) {
        this.jdbcTemplate = jdbcTemplate;
        this.engine = engine;
        this.poolService = poolService;
    }

    @PostConstruct
    public void init() {
        this.executor = Executors.newFixedThreadPool(2);
    }

    public Integer submitAsyncBattle(Integer playerId, String playerTeamJson) {
        // create battle record with nulls to be updated later
        jdbcTemplate.update("INSERT INTO battle(player_id, opponent_team_id, rounds, started_at) VALUES (?, ?, ?, datetime('now'))", playerId, null, 0);
        Integer battleId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);

        executor.submit(() -> runBattle(battleId, playerId, playerTeamJson));
        return battleId;
    }

    private void runBattle(Integer battleId, Integer playerId, String playerTeamJson) {
        try {
            // pick opponent from pool or generate
            String opponentTeamJson = "[]";
            Integer opponentTeamId = null;
            var rows = jdbcTemplate.queryForList("SELECT op.id, t.team_json FROM opponent_pool op JOIN team t ON op.team_id = t.id ORDER BY RANDOM() LIMIT 1");
            if (!rows.isEmpty()) {
                opponentTeamJson = (String) rows.get(0).get("team_json");
                opponentTeamId = (Integer) rows.get(0).get("id");
            } else {
                // generate
                var sample = jdbcTemplate.queryForList("SELECT id, name, base_experience FROM pokemon ORDER BY RANDOM() LIMIT 6");
                opponentTeamJson = mapper.writeValueAsString(sample);
                jdbcTemplate.update("INSERT INTO team(player_id, name, team_json, source, created_at) VALUES (?, ?, ?, 'generated', datetime('now'))", null, "opp-generated-" + System.currentTimeMillis(), opponentTeamJson);
                opponentTeamId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
            }

            Map<String,Object> summary = engine.simulate(playerTeamJson, opponentTeamJson, 10);

            // persist rounds into battle_round
            var rounds = (java.util.List<Map<String,Object>>) summary.get("rounds");
            if (rounds != null) {
                for (Map<String,Object> r : rounds) {
                    try {
                        String logJson = mapper.writeValueAsString(r);
                        jdbcTemplate.update("INSERT INTO battle_round(battle_id, round_number, log_json) VALUES (?, ?, ?)", battleId, (Integer)r.get("round"), logJson);
                    } catch (Exception ignore) {}
                }
            }

            String summaryJson = mapper.writeValueAsString(summary);
            Integer winnerId = "player".equals(summary.get("winner")) ? playerId : null;

            jdbcTemplate.update("UPDATE battle SET opponent_team_id = ?, summary_json = ?, rounds = ?, ended_at = datetime('now'), winner_player_id = ? WHERE id = ?",
                    opponentTeamId, summaryJson, summary.getOrDefault("roundsCount", 0), winnerId, battleId);

            // if player won, add team to pool
            if ("player".equals(summary.get("winner"))) {
                // find player's last team id
                Integer tId = jdbcTemplate.queryForObject("SELECT id FROM team WHERE player_id = ? ORDER BY created_at DESC LIMIT 1", Integer.class, playerId);
                poolService.addTeamToPool(tId, 0);
            }
        } catch (Exception e) {
            try {
                jdbcTemplate.update("UPDATE battle SET summary_json = ?, ended_at = datetime('now') WHERE id = ?", "{\"error\": \"executor_failed\"}", battleId);
            } catch (Exception ignore) {}
        }
    }
}
