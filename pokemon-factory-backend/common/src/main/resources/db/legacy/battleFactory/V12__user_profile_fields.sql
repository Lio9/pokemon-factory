ALTER TABLE app_user ADD COLUMN IF NOT EXISTS display_name TEXT;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS updated_at TEXT;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_login_at TEXT;

UPDATE app_user
SET display_name = COALESCE(NULLIF(display_name, ''), username),
    updated_at = COALESCE(updated_at, created_at, datetime('now'))
WHERE display_name IS NULL
   OR display_name = ''
   OR updated_at IS NULL;
