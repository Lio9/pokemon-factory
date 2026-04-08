ALTER TABLE battle ADD COLUMN player_move_map TEXT;
ALTER TABLE battle ADD COLUMN player_team_json TEXT;
ALTER TABLE battle ADD COLUMN battle_phase TEXT DEFAULT 'team-preview';
