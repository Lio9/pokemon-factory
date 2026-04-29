package com.lio9.common.config;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * 数据库初始化执行器。
 * <p>
 * 设计目标：
 * 1. 只在 common 模块启动时主动初始化数据库；
 * 2. 兼容“全新空库”和“已有旧库”两类场景；
 * 3. 对 battle / user 的补充脚本保持幂等，重复执行也不会破坏已有数据。
 * </p>
 */
@Component
public class CommonDatabaseInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CommonDatabaseInitializer.class);

    private final DataSource dataSource;
    private final CommonDatabaseProperties properties;
    private final CommonCsvDataImporter csvDataImporter;
    private final EffectSeedLoader effectSeedLoader;
    private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

    public CommonDatabaseInitializer(DataSource dataSource, CommonDatabaseProperties properties,
                                     CommonCsvDataImporter csvDataImporter,
                                     EffectSeedLoader effectSeedLoader) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.csvDataImporter = csvDataImporter;
        this.effectSeedLoader = effectSeedLoader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isInitializeOnStartup()) {
            log.debug("当前进程未启用数据库初始化，跳过 common 初始化器。");
            return;
        }

        List<String> scripts = properties.getBootstrapScripts();
        if (scripts == null || scripts.isEmpty()) {
            log.warn("未配置任何数据库初始化脚本，跳过初始化。");
            return;
        }

        log.info("开始执行 common 数据库初始化，共 {} 个脚本。", scripts.size());
        try (Connection connection = dataSource.getConnection()) {
            initializeCoreSchemaIfNeeded(connection, scripts.get(0));

            // 核心库存在后，其余脚本全部按幂等方式补齐当前最终结构。
            for (int i = 1; i < scripts.size(); i++) {
                executeScript(scripts.get(i));
            }

            // SQLite 不支持 "ALTER TABLE ... ADD COLUMN IF NOT EXISTS"，
            // 因此这里用元数据判断列是否存在，再按需补齐老库缺失字段。
            ensureColumnExists(connection, "team", "version", "INTEGER DEFAULT 0");
            ensureColumnExists(connection, "battle", "player_move_map", "TEXT");
            ensureColumnExists(connection, "battle", "player_team_json", "TEXT");
            ensureColumnExists(connection, "battle", "battle_phase", "TEXT DEFAULT 'team-preview'");
            ensureColumnExists(connection, "battle", "factory_run_id", "INTEGER");
            ensureColumnExists(connection, "battle", "run_battle_number", "INTEGER");
            ensureColumnExists(connection, "player", "tier", "INTEGER DEFAULT 0");
            ensureColumnExists(connection, "player", "tier_points", "INTEGER DEFAULT 0");
            ensureColumnExists(connection, "player", "total_points", "INTEGER DEFAULT 0");
            ensureColumnExists(connection, "player", "highest_tier", "INTEGER DEFAULT 0");
            ensureColumnExists(connection, "player", "wins", "INTEGER DEFAULT 0");
            ensureColumnExists(connection, "player", "losses", "INTEGER DEFAULT 0");
            ensureColumnExists(connection, "player", "tier_reached_at", "TEXT");
            ensureColumnExists(connection, "app_user", "display_name", "TEXT");
            ensureColumnExists(connection, "app_user", "updated_at", "TEXT");
            ensureColumnExists(connection, "app_user", "last_login_at", "TEXT");
            ensureTableExists(connection, """
                    CREATE TABLE IF NOT EXISTS ability_effect (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        ability_id INTEGER NOT NULL,
                        effect_type TEXT NOT NULL,
                        effect_value TEXT,
                        target TEXT NOT NULL,
                        condition TEXT,
                        description TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (ability_id) REFERENCES ability(id)
                    )
                    """);
            ensureTableExists(connection, """
                    CREATE TABLE IF NOT EXISTS item_effect (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        item_id INTEGER NOT NULL,
                        effect_type TEXT NOT NULL,
                        effect_value TEXT,
                        target TEXT NOT NULL,
                        condition TEXT,
                        description TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (item_id) REFERENCES item(id)
                    )
                    """);

            // 补充索引（对已有库幂等追加）
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
                try (var stmt = connection.createStatement()) {
                    stmt.execute(ddl);
                } catch (Exception ignored) {}
            }

            csvDataImporter.importIfNeeded(connection);
            effectSeedLoader.syncEffectSeeds(connection);
        }
        log.info("common 数据库初始化完成。");
    }

    /**
     * 只有在核心公共表尚不存在时，才执行全量核心 schema 脚本。
     * <p>
     * 001_core_schema.sql 内部包含 DROP TABLE，用于空库冷启动初始化；
     * 因此这里必须先做存在性判断，避免误伤已经初始化过的数据。
     * 如果数据库文件已经存在，并且内部已经有任意业务表，即使核心表缺失，
     * 也不能贸然执行这份脚本，否则会把现有数据整库覆盖掉。
     * </p>
     */
    private void initializeCoreSchemaIfNeeded(Connection connection, String scriptLocation) throws Exception {
        if (tableExists(connection, "generation") && tableExists(connection, "pokemon") && tableExists(connection, "move")) {
            log.info("检测到核心公共表已存在，跳过核心 schema 初始化。");
            return;
        }

        if (hasExistingBusinessTables(connection)) {
            log.warn("检测到当前数据库已经存在业务表，但核心公共表不完整。为避免覆盖已有数据，跳过 {}。", scriptLocation);
            return;
        }

        log.info("检测到数据库尚未完成核心初始化，开始执行 {}。", scriptLocation);
        executeScript(scriptLocation);
    }

    /**
     * 执行单个 SQL 资源文件。
     */
    private void executeScript(String scriptLocation) {
        Resource resource = resourceLoader.getResource(scriptLocation);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(false, false, "UTF-8", resource);
        populator.execute(dataSource);
        log.info("已执行数据库脚本：{}", scriptLocation);
    }

    /**
     * 判断指定表是否已经存在。
     * <p>
     * 这里直接查询 sqlite_master，而不是依赖业务表本身，
     * 目的是在初始化阶段保持最小依赖，避免“表还没建好时无法判断”的问题。
     * </p>
     */
    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * 判断当前数据库里是否已经存在任意非 sqlite 系统表。
     * <p>
     * 这里的目的不是判断“库是否完备”，而是判断“这是不是一个已经被使用过的库”。
     * 只要存在业务表，就说明库里很可能已经有真实数据，此时必须禁止执行带 DROP TABLE 的冷启动脚本。
     * </p>
     */
    private boolean hasExistingBusinessTables(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        }
    }

    /**
     * 按需为旧表补齐缺失列。
     * <p>
     * 之所以不把这类语句直接写进 SQL 脚本，是因为当前 SQLite 方言并不支持
     * "ALTER TABLE ... ADD COLUMN IF NOT EXISTS"；如果脚本里直接写，会让 common
     * 在全新初始化后仍然启动失败。这里先查表结构，再执行普通 ALTER TABLE，
     * 才能同时兼容空库初始化和旧库升级。
     * </p>
     */
    private void ensureColumnExists(Connection connection, String tableName, String columnName, String columnDefinition) throws Exception {
        if (!tableExists(connection, tableName) || columnExists(connection, tableName, columnName)) {
            return;
        }

        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        log.info("已为表 {} 补齐缺失列 {}。", tableName, columnName);
    }

    private void ensureTableExists(Connection connection, String ddl) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        }
    }

    /**
     * 判断指定列是否已经存在。
     */
    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(" + tableName + ")");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
