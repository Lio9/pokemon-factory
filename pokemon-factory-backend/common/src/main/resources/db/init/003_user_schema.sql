PRAGMA foreign_keys = ON;

-- ============================================================
-- 用户模块最终态表结构
-- 这里直接维护 app_user 的最终字段集合，避免 battleFactory 与 user-module 分别维护同一张表。
-- 旧库缺失字段会由 CommonDatabaseInitializer 在脚本执行后按需补齐，
-- 从而兼容 SQLite 对 ALTER TABLE IF NOT EXISTS 支持不足的问题。
-- ============================================================

CREATE TABLE IF NOT EXISTS app_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    display_name TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
    last_login_at TEXT
);

UPDATE app_user
SET display_name = COALESCE(NULLIF(display_name, ''), username),
    created_at = COALESCE(created_at, datetime('now')),
    updated_at = COALESCE(updated_at, created_at, datetime('now'))
WHERE display_name IS NULL
   OR display_name = ''
   OR created_at IS NULL
   OR updated_at IS NULL;
