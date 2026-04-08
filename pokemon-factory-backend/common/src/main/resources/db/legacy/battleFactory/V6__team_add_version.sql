-- Add version column for optimistic locking on team
ALTER TABLE team RENAME TO team_old;

CREATE TABLE IF NOT EXISTS team (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  player_id INTEGER,
  name TEXT,
  team_json TEXT,
  source TEXT,
  created_at TEXT,
  version INTEGER DEFAULT 0
);

INSERT INTO team (id, player_id, name, team_json, source, created_at, version)
SELECT id, player_id, name, team_json, source, created_at, 0 FROM team_old;

DROP TABLE team_old;
