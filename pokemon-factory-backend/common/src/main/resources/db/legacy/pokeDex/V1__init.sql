-- V1__init.sql - core schema for Pokemon Factory
-- Create essential tables used by the application

-- generation
CREATE TABLE IF NOT EXISTS generation (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  name_en TEXT NOT NULL,
  region TEXT,
  release_year INTEGER,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- type
CREATE TABLE IF NOT EXISTS type (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  name_en TEXT NOT NULL,
  color TEXT,
  icon_url TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ability
CREATE TABLE IF NOT EXISTS ability (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  name_en TEXT NOT NULL,
  description TEXT,
  generation_id INTEGER,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ability_generation FOREIGN KEY (generation_id) REFERENCES generation(id) ON DELETE SET NULL
);

-- move
CREATE TABLE IF NOT EXISTS move (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  name_en TEXT,
  type_id INTEGER,
  power INTEGER,
  pp INTEGER,
  accuracy INTEGER,
  damage_class TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_move_type FOREIGN KEY (type_id) REFERENCES type(id) ON DELETE SET NULL
);

-- pokemon_species
CREATE TABLE IF NOT EXISTS pokemon_species (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  evolution_chain_id INTEGER,
  genus TEXT,
  description TEXT,
  capture_rate INTEGER,
  is_legendary INTEGER DEFAULT 0,
  is_mythical INTEGER DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- pokemon (species instance)
CREATE TABLE IF NOT EXISTS pokemon (
  id INTEGER PRIMARY KEY,
  index_number INTEGER,
  name TEXT NOT NULL,
  name_en TEXT,
  height NUMERIC,
  weight NUMERIC,
  base_experience INTEGER,
  species_id INTEGER,
  generation_id INTEGER,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pokemon_species FOREIGN KEY (species_id) REFERENCES pokemon_species(id) ON DELETE SET NULL,
  CONSTRAINT fk_pokemon_generation FOREIGN KEY (generation_id) REFERENCES generation(id) ON DELETE SET NULL
);

-- pokemon_form
CREATE TABLE IF NOT EXISTS pokemon_form (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  species_id INTEGER NOT NULL,
  form_name TEXT,
  sprite_url TEXT,
  sprite_back_url TEXT,
  sprite_shiny_url TEXT,
  official_artwork_url TEXT,
  base_experience INTEGER,
  height NUMERIC,
  weight NUMERIC,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_form_species FOREIGN KEY (species_id) REFERENCES pokemon_species(id) ON DELETE CASCADE
);

-- pokemon_form_type
CREATE TABLE IF NOT EXISTS pokemon_form_type (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  form_id INTEGER NOT NULL,
  type_id INTEGER NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_form_type_form FOREIGN KEY (form_id) REFERENCES pokemon_form(id) ON DELETE CASCADE,
  CONSTRAINT fk_form_type_type FOREIGN KEY (type_id) REFERENCES type(id) ON DELETE CASCADE,
  UNIQUE (form_id, type_id)
);

-- pokemon_form_ability
CREATE TABLE IF NOT EXISTS pokemon_form_ability (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  form_id INTEGER NOT NULL,
  ability_id INTEGER NOT NULL,
  is_hidden INTEGER DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_form_ability_form FOREIGN KEY (form_id) REFERENCES pokemon_form(id) ON DELETE CASCADE,
  CONSTRAINT fk_form_ability_ability FOREIGN KEY (ability_id) REFERENCES ability(id) ON DELETE CASCADE,
  UNIQUE (form_id, ability_id)
);

-- pokemon_stats
CREATE TABLE IF NOT EXISTS pokemon_stats (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  form_id INTEGER NOT NULL,
  hp INTEGER DEFAULT 0,
  attack INTEGER DEFAULT 0,
  defense INTEGER DEFAULT 0,
  sp_attack INTEGER DEFAULT 0,
  sp_defense INTEGER DEFAULT 0,
  speed INTEGER DEFAULT 0,
  total INTEGER GENERATED ALWAYS AS (hp+attack+defense+sp_attack+sp_defense+speed) VIRTUAL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_stats_form FOREIGN KEY (form_id) REFERENCES pokemon_form(id) ON DELETE CASCADE
);

-- egg_group
CREATE TABLE IF NOT EXISTS egg_group (
  id INTEGER PRIMARY KEY,
  name TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- pokemon_egg_group
CREATE TABLE IF NOT EXISTS pokemon_egg_group (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  species_id INTEGER NOT NULL,
  egg_group_id INTEGER NOT NULL,
  CONSTRAINT fk_pokemon_egg_species FOREIGN KEY (species_id) REFERENCES pokemon_species(id) ON DELETE CASCADE,
  CONSTRAINT fk_pokemon_egg_group FOREIGN KEY (egg_group_id) REFERENCES egg_group(id) ON DELETE CASCADE,
  UNIQUE (species_id, egg_group_id)
);

-- End of migration
