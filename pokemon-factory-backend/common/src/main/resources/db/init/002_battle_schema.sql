PRAGMA foreign_keys = ON;

-- ============================================================
-- 对战工厂最终态表结构
-- 这份脚本不再按历史 Flyway 增量迁移执行，而是直接描述当前 battleFactory 需要的最终表结构。
-- 其中“旧库补列”由 CommonDatabaseInitializer 通过元数据判断后补齐，
-- 避免 SQLite 对 ALTER TABLE IF NOT EXISTS 语法兼容性不足导致初始化失败。
-- ============================================================

CREATE TABLE IF NOT EXISTS player (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    rank INTEGER DEFAULT 0,
    points INTEGER DEFAULT 0,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS team (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id INTEGER,
    name TEXT,
    team_json TEXT,
    source TEXT DEFAULT 'player',
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0,
    FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS opponent_pool (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    team_id INTEGER NOT NULL,
    rank INTEGER DEFAULT 0,
    source TEXT DEFAULT 'pool',
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(team_id) REFERENCES team(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS battle (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id INTEGER NOT NULL,
    opponent_team_id INTEGER,
    started_at TEXT DEFAULT CURRENT_TIMESTAMP,
    ended_at TEXT,
    winner_player_id INTEGER,
    rounds INTEGER DEFAULT 0,
    summary_json TEXT,
    player_move_map TEXT,
    player_team_json TEXT,
    battle_phase TEXT DEFAULT 'team-preview',
    FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE CASCADE,
    FOREIGN KEY(opponent_team_id) REFERENCES team(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS battle_round (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    battle_id INTEGER NOT NULL,
    round_number INTEGER NOT NULL,
    log_json TEXT,
    FOREIGN KEY(battle_id) REFERENCES battle(id) ON DELETE CASCADE
);

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
);

CREATE TABLE IF NOT EXISTS skill_catalog (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    default_cooldown INTEGER DEFAULT 0,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS battle_job (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    battle_id INTEGER,
    status TEXT,
    payload TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_player_username ON player(username);
CREATE INDEX IF NOT EXISTS idx_team_player ON team(player_id);
CREATE INDEX IF NOT EXISTS idx_team_created_at ON team(created_at);
CREATE INDEX IF NOT EXISTS idx_opponent_pool_rank ON opponent_pool(rank);
CREATE INDEX IF NOT EXISTS idx_opponent_pool_team_id ON opponent_pool(team_id);
CREATE INDEX IF NOT EXISTS idx_battle_player ON battle(player_id);
CREATE INDEX IF NOT EXISTS idx_battle_exchange_battle ON battle_exchange(battle_id);

INSERT OR IGNORE INTO skill_catalog(name, default_cooldown) VALUES ('team_shield', 2);
INSERT OR IGNORE INTO skill_catalog(name, default_cooldown) VALUES ('protect', 2);
