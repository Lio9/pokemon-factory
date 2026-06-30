package com.lio9.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Statement;
import java.sql.Connection;

/** Database migration executor 数据库迁移执行器
 * <p>
 * Only performs safe schema migrations (adding missing columns/indexes),
 * NOT full database initialization. Full init is done by Python scripts
 * in the scripts/ directory.
 * 仅执行安全的数据库结构迁移（补充缺失的列和索引），
 * 不进行全量数据库初始化。完整初始化由 scripts/ 目录下的 Python 脚本完成。
 * </p>
 */
@Component
public class CommonDatabaseInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CommonDatabaseInitializer.class);

    private final DataSource dataSource;
    private final CommonDatabaseProperties properties;

    public CommonDatabaseInitializer(DataSource dataSource, CommonDatabaseProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isMigrateOnStartup()) {
            log.debug("Schema migration disabled, skipping.");
            return;
        }

        log.info("Starting schema migration...");
        try (Connection conn = dataSource.getConnection()) {
            // ====== 1. Ensure battle/extension tables exist (if 002 schema wasn't applied yet)
            // 确保对战相关表存在（如果 002 号脚本未执行过）
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS player (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        tier INTEGER DEFAULT 0,
                        tier_points INTEGER DEFAULT 0,
                        total_points INTEGER DEFAULT 0,
                        highest_tier INTEGER DEFAULT 0,
                        wins INTEGER DEFAULT 0,
                        losses INTEGER DEFAULT 0,
                        tier_reached_at TEXT,
                        rank INTEGER DEFAULT 0,
                        points INTEGER DEFAULT 0,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS team (id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_id INTEGER, name TEXT, team_json TEXT, source TEXT DEFAULT 'player',
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP, version INTEGER DEFAULT 0,
                        FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE SET NULL)
                    """);
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS battle (id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_id INTEGER NOT NULL, opponent_team_id INTEGER,
                        factory_run_id INTEGER, run_battle_number INTEGER,
                        started_at TEXT DEFAULT CURRENT_TIMESTAMP, ended_at TEXT,
                        winner_player_id INTEGER, rounds INTEGER DEFAULT 0,
                        summary_json TEXT, player_move_map TEXT, player_team_json TEXT,
                        battle_phase TEXT DEFAULT 'team-preview',
                        FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE CASCADE)
                    """);
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS battle_job (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        battle_id INTEGER,
                        status TEXT,
                        payload TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        updated_at TEXT
                    )
                    """);
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS battle_round (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        battle_id INTEGER NOT NULL,
                        round_number INTEGER NOT NULL,
                        log_json TEXT,
                        FOREIGN KEY(battle_id) REFERENCES battle(id) ON DELETE CASCADE
                    )
                    """);
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS factory_run (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_id INTEGER NOT NULL,
                        current_battle INTEGER DEFAULT 0,
                        max_battles INTEGER DEFAULT 9,
                        wins INTEGER DEFAULT 0, losses INTEGER DEFAULT 0,
                        status TEXT DEFAULT "active", team_json TEXT,
                        tier_at_start INTEGER DEFAULT 0,
                        points_earned INTEGER DEFAULT 0,
                        current_battle_id INTEGER,
                        started_at TEXT DEFAULT CURRENT_TIMESTAMP, ended_at TEXT,
                        FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE CASCADE
                    )
                    """);
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS battle_exchange (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        battle_id INTEGER NOT NULL,
                        player_team_id INTEGER,
                        opponent_team_id INTEGER,
                        replaced_index INTEGER,
                        replaced_pokemon_json TEXT,
                        new_pokemon_json TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(battle_id) REFERENCES battle(id) ON DELETE CASCADE
                    )
                    """);
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS opponent_pool (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        team_id INTEGER NOT NULL,
                        rank INTEGER DEFAULT 0,
                        source TEXT DEFAULT "pool",
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(team_id) REFERENCES team(id) ON DELETE CASCADE
                    )
                    """);
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS skill_catalog (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        default_cooldown INTEGER DEFAULT 0,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS ability_effect (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        ability_id INTEGER NOT NULL, effect_type TEXT NOT NULL,
                        effect_value TEXT, target TEXT NOT NULL, condition TEXT,
                        description TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (ability_id) REFERENCES ability(id)
                    )
                    """);
            ensureTableExists(conn, """
                    CREATE TABLE IF NOT EXISTS item_effect (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        item_id INTEGER NOT NULL, effect_type TEXT NOT NULL,
                        effect_value TEXT, target TEXT NOT NULL, condition TEXT,
                        description TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (item_id) REFERENCES item(id)
                    )
                    """);

            // ====== 2. Ensure columns exist on legacy schemas (migration)
            // 确保旧数据库中缺失的列被补充

            ensureColumnExists(conn, "team", "version", "INTEGER DEFAULT 0");
            ensureColumnExists(conn, "battle", "player_move_map", "TEXT");
            ensureColumnExists(conn, "battle", "player_team_json", "TEXT");
            ensureColumnExists(conn, "battle", "battle_phase", "TEXT DEFAULT 'team-preview'");
            ensureColumnExists(conn, "battle", "factory_run_id", "INTEGER");
            ensureColumnExists(conn, "battle", "run_battle_number", "INTEGER");
            ensureColumnExists(conn, "player", "tier", "INTEGER DEFAULT 0");
            ensureColumnExists(conn, "player", "tier_points", "INTEGER DEFAULT 0");
            ensureColumnExists(conn, "player", "total_points", "INTEGER DEFAULT 0");
            ensureColumnExists(conn, "player", "highest_tier", "INTEGER DEFAULT 0");
            ensureColumnExists(conn, "player", "wins", "INTEGER DEFAULT 0");
            ensureColumnExists(conn, "player", "losses", "INTEGER DEFAULT 0");
            ensureColumnExists(conn, "player", "tier_reached_at", "TEXT");
            ensureColumnExists(conn, "app_user", "display_name", "TEXT");
            ensureColumnExists(conn, "app_user", "updated_at", "TEXT");
            ensureColumnExists(conn, "app_user", "last_login_at", "TEXT");
            ensureColumnExists(conn, "app_user", "failed_attempts", "INTEGER DEFAULT 0");
            ensureColumnExists(conn, "app_user", "locked_until", "TEXT");
            ensureColumnExists(conn, "app_user", "token_version", "INTEGER DEFAULT 1");
            ensureColumnExists(conn, "app_user", "email", "TEXT");
            ensureColumnExists(conn, "app_user", "email_verified", "INTEGER DEFAULT 0");
            ensureColumnExists(conn, "app_user", "verification_token", "TEXT");

            // ====== 3. Ensure indexes exist
            // 确保索引存在（幂等追加）
            String[] extraIndexes = {
                "CREATE INDEX IF NOT EXISTS idx_player_tier_total ON player(tier, total_points DESC)",
                "CREATE INDEX IF NOT EXISTS idx_battle_round_battle ON battle_round(battle_id, round_number)",
                "CREATE INDEX IF NOT EXISTS idx_battle_job_status ON battle_job(status)",
                "CREATE INDEX IF NOT EXISTS idx_factory_run_player_status ON factory_run(player_id, status)",
                "CREATE INDEX IF NOT EXISTS idx_battle_exchange_battle ON battle_exchange(battle_id)",
                "CREATE INDEX IF NOT EXISTS idx_ability_effect_ability ON ability_effect(ability_id)",
                "CREATE INDEX IF NOT EXISTS idx_ability_effect_type ON ability_effect(effect_type)",
                "CREATE INDEX IF NOT EXISTS idx_item_effect_item ON item_effect(item_id)",
                "CREATE INDEX IF NOT EXISTS idx_item_effect_type ON item_effect(effect_type)"
            };
            for (String ddl : extraIndexes) {
                try (var stmt = conn.createStatement()) {
                    stmt.execute(ddl);
                } catch (Exception ex) {
                    log.warn("Failed to create index: {}", ddl, ex);
                }
            }
        }
        log.info("Schema migration completed.");
    }

    /** Check if a column exists in a table, add it if missing. 检查列是否存在，缺失则添加 */
    private void ensureColumnExists(Connection conn, String tableName, String columnName, String columnDefinition) throws Exception {
        if (!tableExists(conn, tableName) || columnExists(conn, tableName, columnName)) {
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
        log.info("Added missing column {}.{}", tableName, columnName);
    }

    /** Create table if not exists (idempotent). 幂等地创建表 */
    private void ensureTableExists(Connection conn, String ddl) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        }
    }

    /** Check if a table exists. 检查表是否存在 */
    private boolean tableExists(Connection conn, String tableName) throws Exception {
        try (var stmt = conn.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            stmt.setString(1, tableName);
            try (var rs = stmt.executeQuery()) { return rs.next(); }
        }
    }

    /** Check if a column exists in a table. 检查列是否存在 */
    private boolean columnExists(Connection conn, String tableName, String columnName) throws Exception {
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) { if (columnName.equalsIgnoreCase(rs.getString("name"))) return true; }
        }
        return false;
    }
}
