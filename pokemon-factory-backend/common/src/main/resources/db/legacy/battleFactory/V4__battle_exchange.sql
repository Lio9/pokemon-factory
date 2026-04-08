-- V4__battle_exchange.sql - record exchanges and history
CREATE TABLE IF NOT EXISTS battle_exchange (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  battle_id INTEGER NOT NULL,
  player_team_id INTEGER,
  opponent_team_id INTEGER,
  replaced_index INTEGER,
  replaced_pokemon_json TEXT,
  new_pokemon_json TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(battle_id) REFERENCES battle(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_battle_exchange_battle ON battle_exchange(battle_id);
