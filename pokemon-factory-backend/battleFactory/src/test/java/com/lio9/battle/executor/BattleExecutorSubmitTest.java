package com.lio9.battle.executor;

import com.lio9.battle.engine.BattleEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BattleExecutorSubmitTest {

    private File dbFile;

    private DataSource createDataSource() throws Exception {
        dbFile = File.createTempFile("test-db-", ".db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl(url);
        return ds;
    }

    private void createSchema(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS battle(id INTEGER PRIMARY KEY AUTOINCREMENT, player_id INTEGER, opponent_team_id INTEGER, rounds INTEGER, player_move_map TEXT, player_team_json TEXT, summary_json TEXT, started_at TEXT, ended_at TEXT, winner_player_id INTEGER);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS team(id INTEGER PRIMARY KEY AUTOINCREMENT, player_id INTEGER, name TEXT, team_json TEXT, source TEXT, created_at TEXT);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS battle_job(id INTEGER PRIMARY KEY AUTOINCREMENT, battle_id INTEGER, status TEXT, payload TEXT, created_at TEXT, updated_at TEXT);");
    }

    @AfterEach
    void cleanup() throws Exception {
        if (dbFile != null && dbFile.exists()) Files.delete(dbFile.toPath());
    }

    @Test
    void submitAsync_createsPendingBattleRow() throws Exception {
        DataSource ds = createDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        createSchema(jdbc);

        BattleEngine engine = new BattleEngine() {
            @Override
            public java.util.Map<String, Object> simulate(String playerTeamJson, String opponentTeamJson, int maxRounds, java.util.Map<String,String> playerMoveMap) {
                return new HashMap<>();
            }
        };

        // create mappers backed by jdbc
        com.lio9.battle.mapper.BattleMapper battleMapper = new com.lio9.battle.mapper.BattleMapper() {
            public void insertInitial(Integer playerId, Integer opponentTeamId, Integer rounds, String playerMoveMapJson, String playerTeamJson) { jdbc.update("INSERT INTO battle(player_id, opponent_team_id, rounds, player_move_map, player_team_json, started_at) VALUES(?, ?, ?, ?, ?, datetime('now'))", playerId, opponentTeamId, rounds, playerMoveMapJson, playerTeamJson); }
            public void insertFinal(Integer playerId, Integer opponentTeamId, Integer rounds, String summaryJson, Integer winnerPlayerId) { jdbc.update("INSERT INTO battle(player_id, opponent_team_id, rounds, summary_json, started_at, ended_at, winner_player_id) VALUES(?, ?, ?, ?, datetime('now'), datetime('now'), ?)", playerId, opponentTeamId, rounds, summaryJson, winnerPlayerId); }
            public Integer lastInsertId() { return jdbc.queryForObject("SELECT last_insert_rowid()", Integer.class); }
            public java.util.Map<String,Object> findBattleWithOpponent(Long id) { java.util.List<java.util.Map<String,Object>> rows = jdbc.queryForList("SELECT b.id, b.player_id, b.opponent_team_id, b.started_at, b.ended_at, b.summary_json, t.team_json AS opponent_team_json, b.player_move_map FROM battle b LEFT JOIN team t ON b.opponent_team_id = t.id WHERE b.id = ?", id); return rows.isEmpty() ? null : rows.get(0); }
            public void updateBattle(Integer id, Integer opponentTeamId, String summaryJson, Integer rounds, Integer winnerPlayerId) { jdbc.update("UPDATE battle SET opponent_team_id = ?, summary_json = ?, rounds = ?, ended_at = datetime('now'), winner_player_id = ? WHERE id = ?", opponentTeamId, summaryJson, rounds, winnerPlayerId, id); }
        };

        com.lio9.battle.mapper.TeamMapper teamMapper = new com.lio9.battle.mapper.TeamMapper() {
            public void insertTeam(Integer playerId, String name, String teamJson, String source) { jdbc.update("INSERT INTO team(player_id, name, team_json, source, created_at) VALUES (?, ?, ?, ?, datetime('now'))", playerId, name, teamJson, source); }
            public Integer lastInsertId() { return jdbc.queryForObject("SELECT last_insert_rowid()", Integer.class); }
            public java.util.Map<String,Object> findLatestByPlayer(Integer playerId) { java.util.List<java.util.Map<String,Object>> rows = jdbc.queryForList("SELECT id, team_json, COALESCE(version,0) AS version FROM team WHERE player_id = ? ORDER BY created_at DESC LIMIT 1", playerId); return rows.isEmpty() ? null : rows.get(0); }
            public String findTeamJsonById(Integer id) { return jdbc.queryForObject("SELECT team_json FROM team WHERE id = ?", String.class, id); }
            public int updateTeamWithVersion(Integer id, String teamJson, Integer expectedVersion) { jdbc.update("UPDATE team SET team_json = ?, version = version + 1 WHERE id = ? AND COALESCE(version,0) = ?", teamJson, id, expectedVersion); return jdbc.queryForObject("SELECT changes()", Integer.class); }
        };

        com.lio9.battle.mapper.PokemonMapper pokemonMapper = new com.lio9.battle.mapper.PokemonMapper() {
            public java.util.List<java.util.Map<String,Object>> sampleLimit(int limit) { return jdbc.queryForList("SELECT id, name, base_experience FROM pokemon ORDER BY RANDOM() LIMIT ?", limit); }
        };

        com.lio9.battle.mapper.OpponentPoolMapper opMapper = new com.lio9.battle.mapper.OpponentPoolMapper() {
            public void addTeam(Integer teamId, Integer rank) { jdbc.update("INSERT INTO opponent_pool(team_id, rank, created_at) VALUES (?, ?, datetime('now'))", teamId, rank); }
            public java.util.List<java.util.Map<String,Object>> sample(int low, int high, int limit) { return jdbc.queryForList("SELECT op.id, t.team_json, op.rank FROM opponent_pool op JOIN team t ON op.team_id = t.id WHERE op.rank BETWEEN ? AND ? ORDER BY RANDOM() LIMIT ?", low, high, limit); }
            public void cleanupOlderThan(String offset) { jdbc.update("DELETE FROM opponent_pool WHERE created_at < datetime('now', ?)", offset); }
        };

        com.lio9.battle.mapper.BattleRoundMapper roundMapper = new com.lio9.battle.mapper.BattleRoundMapper() {
            public void insertRound(Integer battleId, Integer roundNumber, String logJson) { jdbc.update("INSERT INTO battle_round(battle_id, round_number, log_json) VALUES(?, ?, ?)", battleId, roundNumber, logJson); }
        };

        com.lio9.battle.mapper.JobMapper jobMapper = new com.lio9.battle.mapper.JobMapper() {
            public void insertJob(Integer battleId, String status, String payload) { jdbc.update("INSERT INTO battle_job(battle_id, status, payload, created_at) VALUES(?, ?, ?, datetime('now'))", battleId, status, payload); }
            public java.util.List<java.util.Map<String,Object>> findPendingJobs() { return jdbc.queryForList("SELECT id, battle_id, status, payload, created_at FROM battle_job WHERE status = 'PENDING' OR status = 'RUNNING'"); }
            public void updateJobStatus(Integer id, String status) { jdbc.update("UPDATE battle_job SET status = ?, updated_at = datetime('now') WHERE id = ?", status, id); }
        };

        com.lio9.battle.service.OpponentPoolService poolService = new com.lio9.battle.service.OpponentPoolService(opMapper);

        // create a test player
        jdbc.update("CREATE TABLE IF NOT EXISTS player(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, rank INTEGER, points INTEGER);");
        jdbc.update("INSERT OR IGNORE INTO player(username, rank, points) VALUES(?, 0, 0)", "tester");
        Integer playerId = jdbc.queryForObject("SELECT id FROM player WHERE username = ?", Integer.class, "tester");

        com.lio9.battle.service.BattleExecutor executor = new com.lio9.battle.service.BattleExecutor(battleMapper, opMapper, teamMapper, pokemonMapper, roundMapper, jobMapper, engine, poolService);
        executor.init();

        Integer id = executor.submitAsyncBattle(playerId, "[]", null);
        assertNotNull(id);

        Integer countByPlayer = jdbc.queryForObject("SELECT COUNT(1) FROM battle WHERE player_id = ?", Integer.class, playerId);
        assertTrue(countByPlayer != null && countByPlayer.intValue() >= 1);
    }
}
