-- V2: add indexes for opponent pool and team
CREATE INDEX IF NOT EXISTS idx_opponent_pool_team_id ON opponent_pool(team_id);
CREATE INDEX IF NOT EXISTS idx_team_created_at ON team(created_at);
