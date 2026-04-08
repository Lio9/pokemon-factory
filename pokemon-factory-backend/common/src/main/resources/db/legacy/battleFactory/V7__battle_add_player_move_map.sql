-- Flyway migration: add player_move_map to battle (used to persist async player move mappings)
ALTER TABLE battle ADD COLUMN player_move_map TEXT;
