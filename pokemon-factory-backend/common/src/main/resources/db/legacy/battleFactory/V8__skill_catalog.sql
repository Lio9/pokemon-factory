-- Flyway V8: create skill catalog for move metadata
CREATE TABLE IF NOT EXISTS skill_catalog (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE,
  default_cooldown INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);
