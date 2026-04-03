package com.lio9.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.lio9.battle.engine.BattleEngine;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class BattleExecutor {
    private final com.lio9.battle.mapper.BattleMapper battleMapper;
    private final com.lio9.battle.mapper.OpponentPoolMapper opponentPoolMapper;
    private final com.lio9.battle.mapper.TeamMapper teamMapper;
    private final com.lio9.battle.mapper.PokemonMapper pokemonMapper;
    private final com.lio9.battle.mapper.BattleRoundMapper roundMapper;
    private final com.lio9.battle.mapper.JobMapper jobMapper;
    private final BattleEngine engine;
    private final OpponentPoolService poolService;
    private final ObjectMapper mapper = new ObjectMapper();
    private ExecutorService executor;

    public BattleExecutor(com.lio9.battle.mapper.BattleMapper battleMapper, com.lio9.battle.mapper.OpponentPoolMapper opponentPoolMapper, com.lio9.battle.mapper.TeamMapper teamMapper, com.lio9.battle.mapper.PokemonMapper pokemonMapper, com.lio9.battle.mapper.BattleRoundMapper roundMapper, com.lio9.battle.mapper.JobMapper jobMapper, BattleEngine engine, OpponentPoolService poolService) {
        this.battleMapper = battleMapper;
        this.opponentPoolMapper = opponentPoolMapper;
        this.teamMapper = teamMapper;
        this.pokemonMapper = pokemonMapper;
        this.roundMapper = roundMapper;
        this.jobMapper = jobMapper;
        this.engine = engine;
        this.poolService = poolService;
    }

    @PostConstruct
    public void init() {
        this.executor = Executors.newFixedThreadPool(2);
        // recover pending jobs
        try {
            var pending = jobMapper.findPendingJobs();
            if (pending != null) {
                for (var row : pending) {
                    try {
                        Integer bid = row.get("battle_id") == null ? null : ((Number)row.get("battle_id")).intValue();
                        if (bid != null) {
                            // mark running and resubmit
                            Integer jid = row.get("id") == null ? null : ((Number)row.get("id")).intValue();
                            if (jid != null) jobMapper.updateJobStatus(jid, "RUNNING");
                            // attempt to read player id and player team for recovery
                            var bRow = battleMapper.findBattleWithOpponent(bid.longValue());
                            Integer playerId = bRow == null ? null : (Integer)bRow.get("player_id");
                            String playerTeamJson = null;
                            if (playerId != null) {
                                var t = teamMapper.findLatestByPlayer(playerId);
                                if (t != null) playerTeamJson = (String)t.get("team_json");
                            }
                            final String pt = playerTeamJson == null ? "[]" : playerTeamJson;
                            final Integer fb = bid;
                            executor.submit(() -> runBattle(fb, playerId, pt));
                        }
                    } catch (Exception ignore) {}
                }
            }
        } catch (Exception ignore) {}
    }

    public Integer submitAsyncBattle(Integer playerId, String playerTeamJson, String playerMoveMapJson) {
        // create battle record with nulls to be updated later
        // persist playerMoveMap into battle row for async executor
            battleMapper.insertInitial(playerId, null, 0, null);
        Integer battleId = battleMapper.lastInsertId();

        executor.submit(() -> runBattle(battleId, playerId, playerTeamJson)); // playerMoveMapJson will be read by runBattle from DB
        return battleId;
    }

    private void runBattle(Integer battleId, Integer playerId, String playerTeamJson) {
        try {
            // pick opponent from pool or generate
            String opponentTeamJson = "[]";
            Integer opponentTeamId = null;
            var sampleRows = opponentPoolMapper.sample(0, 9999, 1);
            if (sampleRows != null && !sampleRows.isEmpty()) {
                opponentTeamJson = (String) sampleRows.get(0).get("team_json");
                opponentTeamId = (Integer) sampleRows.get(0).get("id");
            } else {
                // generate opponent team from pokemon table
                var sample = pokemonMapper.sampleLimit(6);
                opponentTeamJson = mapper.writeValueAsString(sample);
                teamMapper.insertTeam(null, "opp-generated-" + System.currentTimeMillis(), opponentTeamJson, "generated");
                opponentTeamId = teamMapper.lastInsertId();
            }

            // read persisted playerMoveMap from battle row if present
            Map<String,String> persistedMoves = null;
            try {
                var row = battleMapper.findBattleWithOpponent(battleId.longValue());
                if (row != null && row.containsKey("player_move_map")) {
                    Object pm = row.get("player_move_map");
                    if (pm != null) persistedMoves = mapper.readValue(pm.toString(), Map.class);
                }
            } catch (Exception ignore) {}

            Map<String,Object> summary = null;
            Integer jobId = null;
            try {
                // try to find job for this battle and mark running
                var jobs = jobMapper.findPendingJobs();
                if (jobs != null) {
                    for (var jr : jobs) {
                        Integer bid = jr.get("battle_id") == null ? null : ((Number)jr.get("battle_id")).intValue();
                        if (bid != null && bid.equals(battleId)) {
                            jobId = jr.get("id") == null ? null : ((Number)jr.get("id")).intValue();
                            if (jobId != null) jobMapper.updateJobStatus(jobId, "RUNNING");
                            break;
                        }
                    }
                }
            } catch (Exception ignore) {}

            try {
                summary = engine.simulate(playerTeamJson, opponentTeamJson, 10, persistedMoves);
                if (jobId != null) jobMapper.updateJobStatus(jobId, "DONE");
            } catch (Exception ex) {
                if (jobId != null) jobMapper.updateJobStatus(jobId, "FAILED");
                throw ex;
            }

            // persist rounds into battle_round
            var rounds = (java.util.List<Map<String,Object>>) summary.get("rounds");
            if (rounds != null) {
                for (Map<String,Object> r : rounds) {
                    try {
                        String logJson = mapper.writeValueAsString(r);
                        roundMapper.insertRound(battleId, (Integer)r.get("round"), logJson);
                    } catch (Exception ignore) {}
                }
            }

            String summaryJson = mapper.writeValueAsString(summary);
            Integer winnerId = "player".equals(summary.get("winner")) ? playerId : null;

            battleMapper.updateBattle(battleId, opponentTeamId, summaryJson, (Integer) summary.getOrDefault("roundsCount", 0), winnerId);

            // if player won, add team to pool
            if ("player".equals(summary.get("winner"))) {
                // find player's last team id
                Map<String,Object> teamRow = teamMapper.findLatestByPlayer(playerId);
                Integer tId = teamRow == null ? null : (Integer) teamRow.get("id");
                poolService.addTeamToPool(tId, 0);
            }
        } catch (Exception e) {
            try {
                battleMapper.updateBattle(battleId, null, "{\"error\": \"executor_failed\"}", 0, null);
            } catch (Exception ignore) {}
        }
    }
}
