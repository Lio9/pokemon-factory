-- V3__add_indexes.sql - helpful indexes for common queries

CREATE INDEX IF NOT EXISTS idx_move_name ON move(name);
CREATE INDEX IF NOT EXISTS idx_ability_name ON ability(name);
CREATE INDEX IF NOT EXISTS idx_pokemon_index_number ON pokemon(index_number);
CREATE INDEX IF NOT EXISTS idx_pokemon_name_en ON pokemon(name_en);
CREATE INDEX IF NOT EXISTS idx_pokemon_species_name ON pokemon_species(name);
CREATE INDEX IF NOT EXISTS idx_form_species_idx ON pokemon_form(species_id);

-- Additional compound indexes
CREATE INDEX IF NOT EXISTS idx_form_type_form_type ON pokemon_form_type(form_id, type_id);
CREATE INDEX IF NOT EXISTS idx_form_ability_form_ability ON pokemon_form_ability(form_id, ability_id);
