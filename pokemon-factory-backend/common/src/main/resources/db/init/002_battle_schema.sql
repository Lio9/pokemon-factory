PRAGMA foreign_keys = ON;

-- ============================================================
-- 对战工厂最终态表结构
-- ============================================================

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

CREATE TABLE IF NOT EXISTS factory_run (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id INTEGER NOT NULL,
    current_battle INTEGER DEFAULT 0,
    max_battles INTEGER DEFAULT 9,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    status TEXT DEFAULT 'active',
    team_json TEXT,
    tier_at_start INTEGER DEFAULT 0,
    points_earned INTEGER DEFAULT 0,
    current_battle_id INTEGER,
    started_at TEXT DEFAULT CURRENT_TIMESTAMP,
    ended_at TEXT,
    FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS battle (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id INTEGER NOT NULL,
    opponent_team_id INTEGER,
    factory_run_id INTEGER,
    run_battle_number INTEGER,
    started_at TEXT DEFAULT CURRENT_TIMESTAMP,
    ended_at TEXT,
    winner_player_id INTEGER,
    rounds INTEGER DEFAULT 0,
    summary_json TEXT,
    player_move_map TEXT,
    player_team_json TEXT,
    battle_phase TEXT DEFAULT 'team-preview',
    FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE CASCADE,
    FOREIGN KEY(opponent_team_id) REFERENCES team(id) ON DELETE CASCADE,
    FOREIGN KEY(factory_run_id) REFERENCES factory_run(id) ON DELETE SET NULL
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
CREATE INDEX IF NOT EXISTS idx_player_tier_total ON player(tier, total_points DESC);
CREATE INDEX IF NOT EXISTS idx_team_player ON team(player_id);
CREATE INDEX IF NOT EXISTS idx_team_created_at ON team(created_at);
CREATE INDEX IF NOT EXISTS idx_opponent_pool_rank ON opponent_pool(rank);
CREATE INDEX IF NOT EXISTS idx_opponent_pool_team_id ON opponent_pool(team_id);
CREATE INDEX IF NOT EXISTS idx_battle_player ON battle(player_id);
CREATE INDEX IF NOT EXISTS idx_battle_factory_run ON battle(factory_run_id);
CREATE INDEX IF NOT EXISTS idx_battle_round_battle ON battle_round(battle_id, round_number);
CREATE INDEX IF NOT EXISTS idx_battle_job_status ON battle_job(status);
CREATE INDEX IF NOT EXISTS idx_factory_run_player_status ON factory_run(player_id, status);
CREATE INDEX IF NOT EXISTS idx_battle_exchange_battle ON battle_exchange(battle_id);
CREATE INDEX IF NOT EXISTS idx_battle_exchange_battle ON battle_exchange(battle_id);
CREATE INDEX IF NOT EXISTS idx_factory_run_player ON factory_run(player_id);
CREATE INDEX IF NOT EXISTS idx_factory_run_status ON factory_run(status);

INSERT OR IGNORE INTO skill_catalog(name, default_cooldown) VALUES ('team_shield', 2);
INSERT OR IGNORE INTO skill_catalog(name, default_cooldown) VALUES ('protect', 2);
