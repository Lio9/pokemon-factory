package com.lio9.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

public class BattleServiceStartMatchTest {

    private File dbFile;

    @SuppressWarnings("unchecked")
    private Map<String, Object> summary(Map<String, Object> response) {
        Object value = response.get("summary");
        assertInstanceOf(Map.class, value);
        return (Map<String, Object>) value;
    }

    private DataSource createDataSource() throws Exception {
        dbFile = File.createTempFile("test-db-", ".db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl(url);
        return ds;
    }

    private void createSchema(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS player(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, rank INTEGER, points INTEGER);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS pokemon(id INTEGER PRIMARY KEY, name TEXT, base_experience INTEGER);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS team(id INTEGER PRIMARY KEY AUTOINCREMENT, player_id INTEGER, name TEXT, team_json TEXT, source TEXT, created_at TEXT, version INTEGER DEFAULT 0);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS battle(id INTEGER PRIMARY KEY AUTOINCREMENT, player_id INTEGER, opponent_team_id INTEGER, rounds INTEGER, player_move_map TEXT, player_team_json TEXT, battle_phase TEXT, summary_json TEXT, started_at TEXT, ended_at TEXT, winner_player_id INTEGER);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS battle_job(id INTEGER PRIMARY KEY AUTOINCREMENT, battle_id INTEGER, status TEXT, payload TEXT, created_at TEXT, updated_at TEXT);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS battle_round(id INTEGER PRIMARY KEY AUTOINCREMENT, battle_id INTEGER, round_number INTEGER, log_json TEXT);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS opponent_pool(id INTEGER PRIMARY KEY AUTOINCREMENT, team_id INTEGER, rank INTEGER, created_at TEXT);");
    }

    @AfterEach
    void cleanup() throws Exception {
        if (dbFile != null && dbFile.exists()) Files.delete(dbFile.toPath());
    }

    @Test
    void startMatch_createsBattleAndReturnsSummary() throws Exception {
        DataSource ds = createDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        createSchema(jdbc);
        ObjectMapper objectMapper = new ObjectMapper();

        // insert sample pokemon used for generated team
        jdbc.update("INSERT INTO pokemon(id,name,base_experience) VALUES(?,?,?)", 1, "Poke", 64);

        // simple engine that always makes player win
        BattleEngine engine = new BattleEngine(new com.lio9.battle.service.SkillService(new com.lio9.battle.mapper.SkillMapper() {
                public java.util.List<java.util.Map<String,Object>> findAll() { return java.util.List.of(); }
            }), new com.lio9.pokedex.mapper.TypeEfficacyMapper() {
                public java.util.List<java.util.Map<String, Object>> selectAllTypeEfficacy() { return java.util.List.of(); }
                public java.util.List<java.util.Map<String, Object>> selectByDamageTypeId(Integer damageTypeId) { return java.util.List.of(); }
                public Integer selectDamageFactor(Integer damageTypeId, Integer targetTypeId) { return 100; }
            }, objectMapper) {
            @Override
            public java.util.Map<String, Object> createPreviewState(String playerTeamJson, String opponentTeamJson, int maxRounds, long seed) {
                Map<String, Object> out = new HashMap<>();
                out.put("status", "preview");
                out.put("phase", "team-preview");
                out.put("currentRound", 0);
                out.put("roundLimit", maxRounds);
                out.put("playerTeam", java.util.List.of());
                out.put("opponentTeam", java.util.List.of());
                out.put("playerActiveSlots", java.util.List.of());
                out.put("opponentActiveSlots", java.util.List.of());
                out.put("rounds", java.util.List.of());
                return out;
            }
        };

        // create simple mapper implementations backed by jdbc for testing
        com.lio9.battle.mapper.PlayerMapper playerMapper = new com.lio9.battle.mapper.PlayerMapper() {
            public void insertIgnore(String username) { jdbc.update("INSERT OR IGNORE INTO player(username, rank, points) VALUES(?, 0, 0)", username); }
            public Integer findIdByUsername(String username) { return jdbc.queryForObject("SELECT id FROM player WHERE username = ?", Integer.class, username); }
            public java.util.Map<String, Object> findByUsername(String username) {
                java.util.List<java.util.Map<String, Object>> rows = jdbc.queryForList("SELECT id, username, rank, points FROM player WHERE username = ?", username);
                return rows.isEmpty() ? null : rows.get(0);
            }
            public java.util.Map<String, Object> findById(Integer id) {
                java.util.List<java.util.Map<String, Object>> rows = jdbc.queryForList("SELECT id, username, rank, points FROM player WHERE id = ?", id);
                return rows.isEmpty() ? null : rows.get(0);
            }
            public void updateProgress(Integer playerId, Integer rank, Integer points) {
                jdbc.update("UPDATE player SET rank = ?, points = ? WHERE id = ?", rank, points, playerId);
            }
            public void updateTierProgress(Integer playerId, Integer tier, Integer tierPoints, Integer totalPoints, Integer highestTier, Integer wins, Integer losses) {
                jdbc.update("UPDATE player SET rank = COALESCE(rank, 0) WHERE id = ?", playerId);
            }
            public java.util.List<java.util.Map<String, Object>> leaderboard(Integer limit) { return java.util.List.of(); }
            public java.util.List<java.util.Map<String, Object>> findBattleHistory(Integer playerId, Integer limit) { return java.util.List.of(); }
        };

        com.lio9.battle.mapper.TeamMapper teamMapper = new com.lio9.battle.mapper.TeamMapper() {
            public void insertTeam(Integer playerId, String name, String teamJson, String source) { jdbc.update("INSERT INTO team(player_id, name, team_json, source, created_at) VALUES (?, ?, ?, ?, datetime('now'))", playerId, name, teamJson, source); }
            public Integer lastInsertId() { return jdbc.queryForObject("SELECT id FROM team ORDER BY id DESC LIMIT 1", Integer.class); }
            public java.util.Map<String,Object> findLatestByPlayer(Integer playerId) { java.util.List<java.util.Map<String,Object>> rows = jdbc.queryForList("SELECT id, team_json, COALESCE(version,0) AS version FROM team WHERE player_id = ? ORDER BY created_at DESC LIMIT 1", playerId); return rows.isEmpty() ? null : rows.get(0); }
            public String findTeamJsonById(Integer id) { return jdbc.queryForObject("SELECT team_json FROM team WHERE id = ?", String.class, id); }
            public int updateTeamWithVersion(Integer id, String teamJson, Integer expectedVersion) { jdbc.update("UPDATE team SET team_json = ?, version = version + 1 WHERE id = ? AND COALESCE(version,0) = ?", teamJson, id, expectedVersion); return jdbc.queryForObject("SELECT changes()", Integer.class); }
        };

        com.lio9.battle.mapper.BattleMapper battleMapper = new com.lio9.battle.mapper.BattleMapper() {
            public void insertInitial(Integer playerId, Integer opponentTeamId, Integer rounds, String playerMoveMapJson, String playerTeamJson, String battlePhase, Integer factoryRunId, Integer runBattleNumber) { jdbc.update("INSERT INTO battle(player_id, opponent_team_id, rounds, player_move_map, player_team_json, battle_phase, started_at) VALUES(?, ?, ?, ?, ?, ?, datetime('now'))", playerId, opponentTeamId, rounds, playerMoveMapJson, playerTeamJson, battlePhase); }
            public void insertFinal(Integer playerId, Integer opponentTeamId, Integer rounds, String summaryJson, Integer winnerPlayerId, String battlePhase) { jdbc.update("INSERT INTO battle(player_id, opponent_team_id, rounds, summary_json, battle_phase, started_at, ended_at, winner_player_id) VALUES(?, ?, ?, ?, ?, datetime('now'), datetime('now'), ?)", playerId, opponentTeamId, rounds, summaryJson, battlePhase, winnerPlayerId); }
            public Integer lastInsertId() { return jdbc.queryForObject("SELECT id FROM battle ORDER BY id DESC LIMIT 1", Integer.class); }
            public java.util.Map<String,Object> findBattleWithOpponent(Long id) { java.util.List<java.util.Map<String,Object>> rows = jdbc.queryForList("SELECT b.id, b.player_id, b.opponent_team_id, b.player_move_map, b.player_team_json, b.battle_phase, b.started_at, b.ended_at, b.summary_json, t.team_json AS opponent_team_json FROM battle b LEFT JOIN team t ON b.opponent_team_id = t.id WHERE b.id = ?", id); return rows.isEmpty() ? null : rows.get(0); }
            public void updateBattleState(Integer id, Integer opponentTeamId, String summaryJson, Integer rounds, String battlePhase) { jdbc.update("UPDATE battle SET opponent_team_id = ?, summary_json = ?, rounds = ?, battle_phase = ? WHERE id = ?", opponentTeamId, summaryJson, rounds, battlePhase, id); }
            public void updateBattle(Integer id, Integer opponentTeamId, String summaryJson, Integer rounds, Integer winnerPlayerId, String battlePhase) { jdbc.update("UPDATE battle SET opponent_team_id = ?, summary_json = ?, rounds = ?, battle_phase = ?, ended_at = datetime('now'), winner_player_id = ? WHERE id = ?", opponentTeamId, summaryJson, rounds, battlePhase, winnerPlayerId, id); }
            public void updateBattleTeamState(Integer id, String playerTeamJson, String summaryJson, String battlePhase) { jdbc.update("UPDATE battle SET player_team_json = ?, summary_json = ?, battle_phase = ? WHERE id = ?", playerTeamJson, summaryJson, battlePhase, id); }
        };

        com.lio9.battle.mapper.BattleRoundMapper roundMapper = new com.lio9.battle.mapper.BattleRoundMapper() {
            public void insertRound(Integer battleId, Integer roundNumber, String logJson) {
                // startMatch 测试只验证对战持久化，不依赖 battle_round 明细
            }
        };

        com.lio9.battle.mapper.BattleExchangeMapper exchangeMapper = new com.lio9.battle.mapper.BattleExchangeMapper() {
            public void insertExchange(Long battleId, Integer playerTeamId, Integer opponentTeamId, Integer replacedIndex, String replacedPokemonJson, String newPokemonJson) {
                // startMatch 测试不会触发交换逻辑
            }
        };

        com.lio9.battle.mapper.FactoryRunMapper factoryRunMapper = new com.lio9.battle.mapper.FactoryRunMapper() {
            public void insertRun(Integer playerId, Integer maxBattles, String teamJson, Integer tierAtStart) {}
            public Integer lastInsertId() { return null; }
            public java.util.Map<String, Object> findById(Integer id) { return null; }
            public java.util.Map<String, Object> findActiveRun(Integer playerId) { return null; }
            public void updateProgress(Integer id, Integer currentBattle, Integer wins, Integer losses, Integer currentBattleId, String teamJson) {}
            public void finishRun(Integer id, String status, Integer pointsEarned) {}
            public java.util.List<java.util.Map<String, Object>> findRecentRuns(Integer playerId, Integer limit) { return java.util.List.of(); }
        };

        // simple pool service stub
        com.lio9.battle.service.OpponentPoolService poolService = new com.lio9.battle.service.OpponentPoolService(null) {
            @Override
            public java.util.List<java.util.Map<String, Object>> sample(int rank, int window, int limit) { return java.util.List.of(); }
            @Override
            public void addTeamToPool(Integer teamId, Integer rank) { /* noop for tests */ }
        };

        AIService aiService = new AIService(new com.lio9.battle.mapper.BattleDexMapper() {
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

        BattleService service = new BattleService(playerMapper, teamMapper, battleMapper, roundMapper, exchangeMapper, engine, poolService, aiService, factoryRunMapper, objectMapper);

        Map<String, Object> req = new HashMap<>();
        req.put("username", "tester");
        req.put("teamJson", "[{\"name\":\"Poke\",\"name_en\":\"poke\",\"moves\":[{\"name\":\"tackle\",\"name_en\":\"tackle\",\"power\":40,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1}],\"types\":[{\"type_id\":1}],\"stats\":{\"hp\":120,\"attack\":80,\"defense\":70,\"specialAttack\":60,\"specialDefense\":60,\"speed\":70}}]");

        Map<String, Object> res = service.startMatch(req);
        assertNotNull(res);
        assertTrue(res.containsKey("battleId"));
        assertTrue(res.containsKey("summary"));
        Map<String, Object> summary = summary(res);
        assertEquals("preview", summary.get("status"));
        assertEquals("team-preview", summary.get("phase"));

        Integer battleId = (Integer) res.get("battleId");
        assertNotNull(battleId);

        // verify battle persisted
        Integer count = jdbc.queryForObject("SELECT COUNT(1) FROM battle WHERE id = ?", Integer.class, battleId);
        assertEquals(1, count.intValue());
    }
}
