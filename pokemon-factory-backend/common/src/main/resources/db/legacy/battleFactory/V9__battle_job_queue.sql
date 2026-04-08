-- Flyway V9: job queue for async battle executor
CREATE TABLE IF NOT EXISTS battle_job (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  battle_id INTEGER,
  status TEXT,
  payload TEXT,
  created_at TEXT DEFAULT (datetime('now')),
  updated_at TEXT
);
