package com.lio9.battle.service;

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
        jdbc.execute("CREATE TABLE IF NOT EXISTS battle(id INTEGER PRIMARY KEY AUTOINCREMENT, player_id INTEGER, opponent_team_id INTEGER, rounds INTEGER, player_move_map TEXT, player_team_json TEXT, summary_json TEXT, started_at TEXT, ended_at TEXT, winner_player_id INTEGER);");
        jdbc.execute("CREATE TABLE IF NOT EXISTS battle_job(id INTEGER PRIMARY KEY AUTOINCREMENT, battle_id INTEGER, status TEXT, payload TEXT, created_at TEXT, updated_at TEXT);");
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

        // insert sample pokemon used for generated team
        jdbc.update("INSERT INTO pokemon(id,name,base_experience) VALUES(?,?,?)", 1, "Poke", 64);

        // simple engine that always makes player win
        BattleEngine engine = new BattleEngine(new com.lio9.battle.service.SkillService(new com.lio9.battle.mapper.SkillMapper() {
                public java.util.List<java.util.Map<String,Object>> findAll() { return java.util.List.of(); }
            })) {
            @Override
            public java.util.Map<String, Object> simulate(String playerTeamJson, String opponentTeamJson, int maxRounds, java.util.Map<String,String> playerMoveMap) {
                Map<String, Object> out = new HashMap<>();
                out.put("winner", "player");
                out.put("roundsCount", 1);
                out.put("rounds", java.util.List.of());
                return out;
            }
        };

        // create simple mapper implementations backed by jdbc for testing
        com.lio9.battle.mapper.PlayerMapper playerMapper = new com.lio9.battle.mapper.PlayerMapper() {
            public void insertIgnore(String username) { jdbc.update("INSERT OR IGNORE INTO player(username, rank, points) VALUES(?, 0, 0)", username); }
            public Integer findIdByUsername(String username) { return jdbc.queryForObject("SELECT id FROM player WHERE username = ?", Integer.class, username); }
        };

        com.lio9.battle.mapper.TeamMapper teamMapper = new com.lio9.battle.mapper.TeamMapper() {
            public void insertTeam(Integer playerId, String name, String teamJson, String source) { jdbc.update("INSERT INTO team(player_id, name, team_json, source, created_at) VALUES (?, ?, ?, ?, datetime('now'))", playerId, name, teamJson, source); }
            public Integer lastInsertId() { return jdbc.queryForObject("SELECT id FROM team ORDER BY id DESC LIMIT 1", Integer.class); }
            public java.util.Map<String,Object> findLatestByPlayer(Integer playerId) { java.util.List<java.util.Map<String,Object>> rows = jdbc.queryForList("SELECT id, team_json, COALESCE(version,0) AS version FROM team WHERE player_id = ? ORDER BY created_at DESC LIMIT 1", playerId); return rows.isEmpty() ? null : rows.get(0); }
            public String findTeamJsonById(Integer id) { return jdbc.queryForObject("SELECT team_json FROM team WHERE id = ?", String.class, id); }
            public int updateTeamWithVersion(Integer id, String teamJson, Integer expectedVersion) { jdbc.update("UPDATE team SET team_json = ?, version = version + 1 WHERE id = ? AND COALESCE(version,0) = ?", teamJson, id, expectedVersion); return jdbc.queryForObject("SELECT changes()", Integer.class); }
        };

        com.lio9.battle.mapper.PokemonMapper pokemonMapper = new com.lio9.battle.mapper.PokemonMapper() {
            public java.util.List<java.util.Map<String,Object>> sampleLimit(int limit) { return jdbc.queryForList("SELECT id, name, base_experience FROM pokemon LIMIT ?", limit); }
        };

        com.lio9.battle.mapper.BattleMapper battleMapper = new com.lio9.battle.mapper.BattleMapper() {
            public void insertInitial(Integer playerId, Integer opponentTeamId, Integer rounds, String playerMoveMapJson, String playerTeamJson) { jdbc.update("INSERT INTO battle(player_id, opponent_team_id, rounds, player_move_map, player_team_json, started_at) VALUES(?, ?, ?, ?, ?, datetime('now'))", playerId, opponentTeamId, rounds, playerMoveMapJson, playerTeamJson); }
            public void insertFinal(Integer playerId, Integer opponentTeamId, Integer rounds, String summaryJson, Integer winnerPlayerId) { jdbc.update("INSERT INTO battle(player_id, opponent_team_id, rounds, summary_json, started_at, ended_at, winner_player_id) VALUES(?, ?, ?, ?, datetime('now'), datetime('now'), ?)", playerId, opponentTeamId, rounds, summaryJson, winnerPlayerId); }
            public Integer lastInsertId() { return jdbc.queryForObject("SELECT id FROM battle ORDER BY id DESC LIMIT 1", Integer.class); }
            public java.util.Map<String,Object> findBattleWithOpponent(Long id) { java.util.List<java.util.Map<String,Object>> rows = jdbc.queryForList("SELECT b.id, b.player_id, b.opponent_team_id, b.started_at, b.ended_at, b.summary_json, t.team_json AS opponent_team_json, b.player_move_map FROM battle b LEFT JOIN team t ON b.opponent_team_id = t.id WHERE b.id = ?", id); return rows.isEmpty() ? null : rows.get(0); }
            public void updateBattle(Integer id, Integer opponentTeamId, String summaryJson, Integer rounds, Integer winnerPlayerId) { jdbc.update("UPDATE battle SET opponent_team_id = ?, summary_json = ?, rounds = ?, ended_at = datetime('now'), winner_player_id = ? WHERE id = ?", opponentTeamId, summaryJson, rounds, winnerPlayerId, id); }
        };

        // simple pool service stub
        com.lio9.battle.service.OpponentPoolService poolService = new com.lio9.battle.service.OpponentPoolService(null) {
            @Override
            public java.util.List<java.util.Map<String, Object>> sample(int rank, int window, int limit) { return java.util.List.of(); }
            @Override
            public void addTeamToPool(Integer teamId, Integer rank) { /* noop for tests */ }
        };

        BattleService service = new BattleService(playerMapper, teamMapper, pokemonMapper, battleMapper, engine, poolService);

        Map<String, Object> req = new HashMap<>();
        req.put("username", "tester");
        req.put("teamJson", null);

        Map<String, Object> res = service.startMatch(req);
        assertNotNull(res);
        assertTrue(res.containsKey("battleId"));
        assertTrue(res.containsKey("summary"));

        Integer battleId = (Integer) res.get("battleId");
        assertNotNull(battleId);

        // verify battle persisted
        Integer count = jdbc.queryForObject("SELECT COUNT(1) FROM battle WHERE id = ?", Integer.class, battleId);
        assertEquals(1, count.intValue());
    }
}
