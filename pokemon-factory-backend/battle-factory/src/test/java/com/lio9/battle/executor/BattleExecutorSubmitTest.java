package com.lio9.battle.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.engine.BattleEngine;
import com.lio9.battle.service.AIService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class BattleExecutorSubmitTest {

    private File dbFile;
    private com.lio9.battle.service.BattleExecutor executorUnderTest;

    private DataSource createDataSource() throws Exception {
        dbFile = File.createTempFile("test-db-", ".db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl(url);
        return ds;
    }

    private void createSchema(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS battle(id INTEGER PRIMARY KEY AUTOINCREMENT, player_id INTEGER, opponent_team_id INTEGER, rounds INTEGER, player_move_map TEXT, player_team_json TEXT, battle_phase TEXT, summary_json TEXT, started_at TEXT, ended_at TEXT, winner_player_id INTEGER);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS team(id INTEGER PRIMARY KEY AUTOINCREMENT, player_id INTEGER, name TEXT, team_json TEXT, source TEXT, created_at TEXT, version INTEGER DEFAULT 0);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS battle_job(id INTEGER PRIMARY KEY AUTOINCREMENT, battle_id INTEGER, status TEXT, payload TEXT, created_at TEXT, updated_at TEXT);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS battle_round(id INTEGER PRIMARY KEY AUTOINCREMENT, battle_id INTEGER, round_number INTEGER, log_json TEXT);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS opponent_pool(id INTEGER PRIMARY KEY AUTOINCREMENT, team_id INTEGER, rank INTEGER, created_at TEXT);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS pokemon(id INTEGER PRIMARY KEY, name TEXT, base_experience INTEGER);");
    }

    @AfterEach
    void cleanup() throws Exception {
        shutdownExecutor();
        deleteTempDb();
    }

    private void shutdownExecutor() throws Exception {
        if (executorUnderTest == null) {
            return;
        }

        Field executorField = executorUnderTest.getClass().getDeclaredField("executor");
        executorField.setAccessible(true);
        ExecutorService executor = (ExecutorService) executorField.get(executorUnderTest);
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void deleteTempDb() throws Exception {
        if (dbFile == null || !dbFile.exists()) {
            return;
        }

        Path path = dbFile.toPath();
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (java.nio.file.FileSystemException ex) {
                if (attempt == 4) {
                    throw ex;
                }
                Thread.sleep(200L);
            }
        }
    }

    @Test
    void submitAsync_createsPendingBattleRow() throws Exception {
        DataSource ds = createDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        createSchema(jdbc);
        ObjectMapper objectMapper = new ObjectMapper();

        BattleEngine engine = new BattleEngine(new com.lio9.battle.service.SkillService(new com.lio9.battle.mapper.SkillMapper() {
                public java.util.List<java.util.Map<String,Object>> findAll() { return java.util.List.of(); }
            }), new com.lio9.pokedex.mapper.TypeEfficacyMapper() {
                public java.util.List<java.util.Map<String, Object>> selectAllTypeEfficacy() { return java.util.List.of(); }
                public java.util.List<java.util.Map<String, Object>> selectByDamageTypeId(Integer damageTypeId) { return java.util.List.of(); }
                public Integer selectDamageFactor(Integer damageTypeId, Integer targetTypeId) { return 100; }
            }, objectMapper) {
            @Override
            public java.util.Map<String, Object> createBattleState(String playerTeamJson, String opponentTeamJson, int maxRounds, long seed) {
                java.util.Map<String, Object> state = new HashMap<>();
                state.put("status", "running");
                state.put("phase", "battle");
                state.put("winner", null);
                state.put("playerTeam", java.util.List.of());
                state.put("rounds", new java.util.ArrayList<java.util.Map<String, Object>>());
                return state;
            }

            @Override
            public java.util.Map<String, Object> autoPlay(java.util.Map<String, Object> rawState, java.util.Map<String,String> playerMoveMap) {
                java.util.Map<String, Object> state = new HashMap<>(rawState);
                state.put("status", "completed");
                state.put("phase", "completed");
                state.put("winner", "player");
                state.put("playerTeam", java.util.List.of());
                state.put("rounds", new java.util.ArrayList<java.util.Map<String, Object>>());
                return state;
            }
        };

        // create mappers backed by jdbc
        com.lio9.battle.mapper.BattleMapper battleMapper = new com.lio9.battle.mapper.BattleMapper() {
            public void insertInitial(Integer playerId, Integer opponentTeamId, Integer rounds, String playerMoveMapJson, String playerTeamJson, String battlePhase, Integer factoryRunId, Integer runBattleNumber) { jdbc.update("INSERT INTO battle(player_id, opponent_team_id, rounds, player_move_map, player_team_json, battle_phase, started_at) VALUES(?, ?, ?, ?, ?, ?, datetime('now'))", playerId, opponentTeamId, rounds, playerMoveMapJson, playerTeamJson, battlePhase); }
            public void insertFinal(Integer playerId, Integer opponentTeamId, Integer rounds, String summaryJson, Integer winnerPlayerId, String battlePhase) { jdbc.update("INSERT INTO battle(player_id, opponent_team_id, rounds, summary_json, battle_phase, started_at, ended_at, winner_player_id) VALUES(?, ?, ?, ?, ?, datetime('now'), datetime('now'), ?)", playerId, opponentTeamId, rounds, summaryJson, battlePhase, winnerPlayerId); }
            public Integer lastInsertId() { return jdbc.queryForObject("SELECT last_insert_rowid()", Integer.class); }
            public java.util.Map<String,Object> findBattleWithOpponent(Long id) { java.util.List<java.util.Map<String,Object>> rows = jdbc.queryForList("SELECT b.id, b.player_id, b.opponent_team_id, b.player_move_map, b.player_team_json, b.battle_phase, b.started_at, b.ended_at, b.summary_json, t.team_json AS opponent_team_json FROM battle b LEFT JOIN team t ON b.opponent_team_id = t.id WHERE b.id = ?", id); return rows.isEmpty() ? null : rows.get(0); }
            public void updateBattleState(Integer id, Integer opponentTeamId, String summaryJson, Integer rounds, String battlePhase) { jdbc.update("UPDATE battle SET opponent_team_id = ?, summary_json = ?, rounds = ?, battle_phase = ? WHERE id = ?", opponentTeamId, summaryJson, rounds, battlePhase, id); }
            public void updateBattle(Integer id, Integer opponentTeamId, String summaryJson, Integer rounds, Integer winnerPlayerId, String battlePhase) { jdbc.update("UPDATE battle SET opponent_team_id = ?, summary_json = ?, rounds = ?, battle_phase = ?, ended_at = datetime('now'), winner_player_id = ? WHERE id = ?", opponentTeamId, summaryJson, rounds, battlePhase, winnerPlayerId, id); }
            public void updateBattleTeamState(Integer id, String playerTeamJson, String summaryJson, String battlePhase) { jdbc.update("UPDATE battle SET player_team_json = ?, summary_json = ?, battle_phase = ? WHERE id = ?", playerTeamJson, summaryJson, battlePhase, id); }
        };

        com.lio9.battle.mapper.TeamMapper teamMapper = new com.lio9.battle.mapper.TeamMapper() {
            public void insertTeam(Integer playerId, String name, String teamJson, String source) { jdbc.update("INSERT INTO team(player_id, name, team_json, source, created_at) VALUES (?, ?, ?, ?, datetime('now'))", playerId, name, teamJson, source); }
            public Integer lastInsertId() { return jdbc.queryForObject("SELECT last_insert_rowid()", Integer.class); }
            public java.util.Map<String,Object> findLatestByPlayer(Integer playerId) { java.util.List<java.util.Map<String,Object>> rows = jdbc.queryForList("SELECT id, team_json, COALESCE(version,0) AS version FROM team WHERE player_id = ? ORDER BY created_at DESC LIMIT 1", playerId); return rows.isEmpty() ? null : rows.get(0); }
            public String findTeamJsonById(Integer id) { return jdbc.queryForObject("SELECT team_json FROM team WHERE id = ?", String.class, id); }
            public int updateTeamWithVersion(Integer id, String teamJson, Integer expectedVersion) { jdbc.update("UPDATE team SET team_json = ?, version = version + 1 WHERE id = ? AND COALESCE(version,0) = ?", teamJson, id, expectedVersion); return jdbc.queryForObject("SELECT changes()", Integer.class); }
        };

        com.lio9.battle.mapper.OpponentPoolMapper opMapper = new com.lio9.battle.mapper.OpponentPoolMapper() {
            public void addTeam(Integer teamId, Integer rank) { jdbc.update("INSERT INTO opponent_pool(team_id, rank, created_at) VALUES (?, ?, datetime('now'))", teamId, rank); }
            public java.util.List<java.util.Map<String,Object>> sample(int low, int high, int limit, int targetRank) { return jdbc.queryForList("SELECT op.id, op.team_id AS team_id, t.team_json, op.rank FROM opponent_pool op JOIN team t ON op.team_id = t.id WHERE op.rank BETWEEN ? AND ? ORDER BY ABS(op.rank - ?) ASC, RANDOM() LIMIT ?", low, high, targetRank, limit); }
            public void cleanupOlderThan(String offset) { jdbc.update("DELETE FROM opponent_pool WHERE created_at < datetime('now', ?)", offset); }
        };

        com.lio9.battle.mapper.BattleRoundMapper roundMapper = new com.lio9.battle.mapper.BattleRoundMapper() {
            public void insertRound(Integer battleId, Integer roundNumber, String logJson) { jdbc.update("INSERT INTO battle_round(battle_id, round_number, log_json) VALUES(?, ?, ?)", battleId, roundNumber, logJson); }
        };

        com.lio9.battle.mapper.JobMapper jobMapper = new com.lio9.battle.mapper.JobMapper() {
            public void insertJob(Integer battleId, String status, String payload) { jdbc.update("INSERT INTO battle_job(battle_id, status, payload, created_at) VALUES(?, ?, ?, datetime('now'))", battleId, status, payload); }
            public java.util.List<java.util.Map<String,Object>> findPendingJobs() { return jdbc.queryForList("SELECT id, battle_id, status, payload, created_at FROM battle_job WHERE status = 'PENDING' OR status = 'RUNNING'"); }
            public void updateJobStatus(Integer id, String status) { jdbc.update("UPDATE battle_job SET status = ?, updated_at = datetime('now') WHERE id = ?", status, id); }
            public void updateJobStatusByBattleId(Integer battleId, String status) { jdbc.update("UPDATE battle_job SET status = ?, updated_at = datetime('now') WHERE battle_id = ? AND (status = 'PENDING' OR status = 'RUNNING')", status, battleId); }
        };

        com.lio9.battle.service.OpponentPoolService poolService = new com.lio9.battle.service.OpponentPoolService(opMapper);

        // create a test player
        jdbc.update("CREATE TABLE IF NOT EXISTS player(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, rank INTEGER, points INTEGER);");
        jdbc.update("INSERT OR IGNORE INTO player(username, rank, points) VALUES(?, 0, 0)", "tester");
        Integer playerId = jdbc.queryForObject("SELECT id FROM player WHERE username = ?", Integer.class, "tester");

        AIService aiService = new AIService(
            new com.lio9.battle.config.BattleConfig(),
            new com.lio9.battle.engine.TeamBalanceEvaluator(),
            new com.lio9.battle.mapper.BattleDexMapper() {
            public java.util.List<java.util.Map<String, Object>> selectRandomDefaultForms(int limit) { return java.util.List.of(); }
            public java.util.List<java.util.Map<String, Object>> selectFormStats(Integer formId) { return java.util.List.of(); }
            public java.util.List<java.util.Map<String, Object>> selectFormTypes(Integer formId) { return java.util.List.of(); }
            public java.util.List<java.util.Map<String, Object>> selectFormAbilities(Integer formId) { return java.util.List.of(); }
            public java.util.List<java.util.Map<String, Object>> selectCompetitiveMoves(Integer formId, int limit) { return java.util.List.of(); }
        }, objectMapper) {
            @Override
            public String generateFactoryTeamJson(int size, int rank, long seed, java.util.Set<String> excludedNames) {
                return "[{\"name\":\"Poke\",\"name_en\":\"poke\",\"moves\":[{\"name\":\"tackle\",\"name_en\":\"tackle\",\"power\":40,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1}],\"types\":[{\"type_id\":1}],\"stats\":{\"hp\":120,\"attack\":80,\"defense\":70,\"specialAttack\":60,\"specialDefense\":60,\"speed\":70}}]";
            }
        };

        executorUnderTest = new com.lio9.battle.service.BattleExecutor(battleMapper, teamMapper, roundMapper, jobMapper, engine, poolService, aiService, objectMapper);
        executorUnderTest.init();

        Integer id = executorUnderTest.submitAsyncBattle(playerId, "[]", null);
        assertNotNull(id);

        Integer countByPlayer = jdbc.queryForObject("SELECT COUNT(1) FROM battle WHERE player_id = ?", Integer.class, playerId);
        assertTrue(countByPlayer != null && countByPlayer.intValue() >= 1);

        Integer jobCount = jdbc.queryForObject("SELECT COUNT(1) FROM battle_job WHERE battle_id = ?", Integer.class, id);
        assertEquals(1, jobCount);
    }
}
