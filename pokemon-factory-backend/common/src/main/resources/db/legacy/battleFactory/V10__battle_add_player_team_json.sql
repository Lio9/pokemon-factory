-- Flyway V10: add player_team_json to battle to persist player's team with async jobs
ALTER TABLE battle ADD COLUMN player_team_json TEXT;
