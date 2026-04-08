-- V1__battle_init.sql - core tables for battleFactory (SQLite compatible)

-- players stores minimal player info and ranking
CREATE TABLE IF NOT EXISTS player (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE,
  rank INTEGER DEFAULT 0,
  points INTEGER DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- teams: a stored configured team (4-6 pokemon) belonging to player (or null for generated teams)
CREATE TABLE IF NOT EXISTS team (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  player_id INTEGER,
  name TEXT,
  team_json TEXT NOT NULL, -- JSON blob containing pokemon list and full config
  source TEXT DEFAULT 'player', -- 'player' | 'generated' | 'pool'
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE SET NULL
);

-- opponent_pool: store winning teams for matching
CREATE TABLE IF NOT EXISTS opponent_pool (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  team_id INTEGER NOT NULL,
  rank INTEGER DEFAULT 0,
  source TEXT DEFAULT 'pool',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(team_id) REFERENCES team(id) ON DELETE CASCADE
);

-- battles: high level battle record
CREATE TABLE IF NOT EXISTS battle (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  player_id INTEGER NOT NULL,
  opponent_team_id INTEGER,
  started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  ended_at DATETIME,
  winner_player_id INTEGER,
  rounds INTEGER DEFAULT 0,
  summary_json TEXT, -- results, picks, exchanges
  FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE CASCADE,
  FOREIGN KEY(opponent_team_id) REFERENCES team(id) ON DELETE CASCADE
);

-- battle_rounds: per-round logs for replay/analysis
CREATE TABLE IF NOT EXISTS battle_round (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  battle_id INTEGER NOT NULL,
  round_number INTEGER NOT NULL,
  log_json TEXT, -- actions/outcomes for the round
  FOREIGN KEY(battle_id) REFERENCES battle(id) ON DELETE CASCADE
);

-- indexes for common queries
CREATE INDEX IF NOT EXISTS idx_player_username ON player(username);
CREATE INDEX IF NOT EXISTS idx_team_player ON team(player_id);
CREATE INDEX IF NOT EXISTS idx_opponent_pool_rank ON opponent_pool(rank);
CREATE INDEX IF NOT EXISTS idx_battle_player ON battle(player_id);

-- notes: team_json and summary_json are JSON blobs to allow flexible extension (moves, iv/ev, items, etc.)
