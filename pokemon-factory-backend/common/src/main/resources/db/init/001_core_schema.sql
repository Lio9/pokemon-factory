-- ============================================================
-- 宝可梦工厂 - SQLite数据库初始化脚本
-- 从MySQL转换而来，适配SQLite语法
-- ============================================================

PRAGMA foreign_keys = OFF;

-- ==========================================
-- 一、基础数据表 (无外键依赖)
-- ==========================================

-- 世代表
DROP TABLE IF EXISTS generation;
CREATE TABLE generation (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    region TEXT,
    release_year INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 属性表
DROP TABLE IF EXISTS type;
CREATE TABLE type (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    name_en TEXT NOT NULL UNIQUE,
    name_jp TEXT,
    color TEXT,
    icon_url TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 属性相性表 (伤害倍率)
DROP TABLE IF EXISTS type_efficacy;
CREATE TABLE type_efficacy (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    attacking_type_id INTEGER NOT NULL,
    defending_type_id INTEGER NOT NULL,
    damage_factor INTEGER NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (attacking_type_id) REFERENCES type(id),
    FOREIGN KEY (defending_type_id) REFERENCES type(id),
    UNIQUE (attacking_type_id, defending_type_id)
);

-- 特性表
DROP TABLE IF EXISTS ability;
CREATE TABLE ability (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    name_jp TEXT,
    description TEXT,
    description_en TEXT,
    effect_detail TEXT,
    generation_id INTEGER,
    is_main_series INTEGER DEFAULT 1,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (generation_id) REFERENCES generation(id)
);

-- 技能伤害类型表
DROP TABLE IF EXISTS move_damage_class;
CREATE TABLE move_damage_class (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 种族值类型表
DROP TABLE IF EXISTS stat;
CREATE TABLE stat (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    name_jp TEXT,
    is_battle_only INTEGER DEFAULT 0,
    game_index INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 性别表
DROP TABLE IF EXISTS gender;
CREATE TABLE gender (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 性格表
DROP TABLE IF EXISTS nature;
CREATE TABLE nature (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    name_jp TEXT,
    increased_stat TEXT,
    decreased_stat TEXT,
    likes_flavor TEXT,
    hates_flavor TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 蛋群表
DROP TABLE IF EXISTS egg_group;
CREATE TABLE egg_group (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    name_jp TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 经验成长类型表
DROP TABLE IF EXISTS growth_rate;
CREATE TABLE growth_rate (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    formula TEXT,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 技能学习方式表
DROP TABLE IF EXISTS move_learn_method;
CREATE TABLE move_learn_method (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 进化触发条件表
DROP TABLE IF EXISTS evolution_trigger;
CREATE TABLE evolution_trigger (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 异常状态表
DROP TABLE IF EXISTS move_meta_ailment;
CREATE TABLE move_meta_ailment (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 技能元数据类别表
DROP TABLE IF EXISTS move_meta_category;
CREATE TABLE move_meta_category (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 技能目标类型表
DROP TABLE IF EXISTS move_target;
CREATE TABLE move_target (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 物品口袋表
DROP TABLE IF EXISTS item_pocket;
CREATE TABLE item_pocket (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 物品投掷效果表
DROP TABLE IF EXISTS item_fling_effect;
CREATE TABLE item_fling_effect (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 物品属性表
DROP TABLE IF EXISTS item_flag;
CREATE TABLE item_flag (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 二、版本相关表
-- ==========================================

-- 版本组表
DROP TABLE IF EXISTS version_group;
CREATE TABLE version_group (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    generation_id INTEGER,
    "order" INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (generation_id) REFERENCES generation(id)
);

-- 物品分类表
DROP TABLE IF EXISTS item_category;
CREATE TABLE item_category (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    pocket_id INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pocket_id) REFERENCES item_pocket(id)
);

-- ==========================================
-- 三、技能与物品表
-- ==========================================

-- 技能表
DROP TABLE IF EXISTS move;
CREATE TABLE move (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    name_jp TEXT,
    type_id INTEGER NOT NULL,
    damage_class_id INTEGER,
    target_id INTEGER,
    power INTEGER,
    pp INTEGER,
    accuracy INTEGER,
    priority INTEGER DEFAULT 0,
    effect_chance INTEGER,
    effect_short TEXT,
    effect_detail TEXT,
    description TEXT,
    generation_id INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (type_id) REFERENCES type(id),
    FOREIGN KEY (damage_class_id) REFERENCES move_damage_class(id),
    FOREIGN KEY (target_id) REFERENCES move_target(id),
    FOREIGN KEY (generation_id) REFERENCES generation(id)
);

-- 技能元数据表 (连击、吸取等)
DROP TABLE IF EXISTS move_meta;
CREATE TABLE move_meta (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    move_id INTEGER NOT NULL,
    min_hits INTEGER,
    max_hits INTEGER,
    min_turns INTEGER,
    max_turns INTEGER,
    drain INTEGER,
    healing INTEGER,
    crit_rate INTEGER,
    ailment_chance INTEGER,
    flinch_chance INTEGER,
    stat_chance INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (move_id) REFERENCES move(id) ON DELETE CASCADE,
    UNIQUE (move_id)
);

-- 技能能力变化表
DROP TABLE IF EXISTS move_meta_stat_change;
CREATE TABLE move_meta_stat_change (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    move_id INTEGER NOT NULL,
    stat_id INTEGER NOT NULL,
    "change" INTEGER NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (move_id) REFERENCES move(id) ON DELETE CASCADE,
    FOREIGN KEY (stat_id) REFERENCES stat(id)
);

-- 技能标记表 (定义技能特性标记)
DROP TABLE IF EXISTS move_flags;
CREATE TABLE move_flags (
    id INTEGER PRIMARY KEY,
    identifier TEXT NOT NULL UNIQUE,
    name TEXT,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 技能-标记关联表
DROP TABLE IF EXISTS move_flag_map;
CREATE TABLE move_flag_map (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    move_id INTEGER NOT NULL,
    flag_id INTEGER NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (move_id) REFERENCES move(id) ON DELETE CASCADE,
    FOREIGN KEY (flag_id) REFERENCES move_flags(id),
    UNIQUE (move_id, flag_id)
);

-- 物品表
DROP TABLE IF EXISTS item;
CREATE TABLE item (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    name_jp TEXT,
    category_id INTEGER,
    cost INTEGER DEFAULT 0,
    fling_power INTEGER,
    fling_effect_id INTEGER,
    effect_short TEXT,
    effect_detail TEXT,
    description TEXT,
    generation_id INTEGER,
    sprite_url TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES item_category(id),
    FOREIGN KEY (fling_effect_id) REFERENCES item_fling_effect(id),
    FOREIGN KEY (generation_id) REFERENCES generation(id)
);

-- 物品-属性关联表
DROP TABLE IF EXISTS item_flag_map;
CREATE TABLE item_flag_map (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    flag_id INTEGER NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE,
    FOREIGN KEY (flag_id) REFERENCES item_flag(id),
    UNIQUE (item_id, flag_id)
);

-- 特性效果表
DROP TABLE IF EXISTS ability_effect;
CREATE TABLE ability_effect (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ability_id INTEGER NOT NULL,
    effect_type TEXT NOT NULL,
    effect_value TEXT,
    target TEXT NOT NULL,
    condition TEXT,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ability_id) REFERENCES ability(id)
);

-- 道具效果表
DROP TABLE IF EXISTS item_effect;
CREATE TABLE item_effect (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    effect_type TEXT NOT NULL,
    effect_value TEXT,
    target TEXT NOT NULL,
    condition TEXT,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES item(id)
);

-- ==========================================
-- 四、宝可梦核心表
-- ==========================================

-- 进化链表
DROP TABLE IF EXISTS evolution_chain;
CREATE TABLE evolution_chain (
    id INTEGER PRIMARY KEY,
    baby_trigger_item_id INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (baby_trigger_item_id) REFERENCES item(id)
);

-- 宝可梦物种表 (图鉴核心)
DROP TABLE IF EXISTS pokemon_species;
CREATE TABLE pokemon_species (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL UNIQUE,
    name_jp TEXT,
    genus TEXT,
    generation_id INTEGER,
    evolution_chain_id INTEGER,
    evolves_from_species_id INTEGER,
    color TEXT,
    shape TEXT,
    habitat TEXT,
    growth_rate_id INTEGER,
    gender_rate INTEGER DEFAULT -1,
    capture_rate INTEGER DEFAULT 0,
    base_happiness INTEGER DEFAULT 70,
    hatch_counter INTEGER,
    is_baby INTEGER DEFAULT 0,
    is_legendary INTEGER DEFAULT 0,
    is_mythical INTEGER DEFAULT 0,
    has_gender_differences INTEGER DEFAULT 0,
    forms_switchable INTEGER DEFAULT 0,
    "order" INTEGER,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (generation_id) REFERENCES generation(id),
    FOREIGN KEY (evolution_chain_id) REFERENCES evolution_chain(id),
    FOREIGN KEY (evolves_from_species_id) REFERENCES pokemon_species(id),
    FOREIGN KEY (growth_rate_id) REFERENCES growth_rate(id)
);

-- 宝可梦形态表 (对战核心)
DROP TABLE IF EXISTS pokemon_form;
CREATE TABLE pokemon_form (
    id INTEGER PRIMARY KEY,
    species_id INTEGER NOT NULL,
    form_name TEXT,
    form_name_zh TEXT,
    form_name_jp TEXT,
    is_default INTEGER DEFAULT 1,
    is_battle_only INTEGER DEFAULT 0,
    is_mega INTEGER DEFAULT 0,
    is_gigantamax INTEGER DEFAULT 0,
    is_terastal INTEGER DEFAULT 0,
    height REAL,
    weight REAL,
    base_experience INTEGER,
    "order" INTEGER,
    sprite_url TEXT,
    sprite_back_url TEXT,
    sprite_shiny_url TEXT,
    sprite_shiny_back_url TEXT,
    official_artwork_url TEXT,
    cry_url TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (species_id) REFERENCES pokemon_species(id) ON DELETE CASCADE
);

-- ==========================================
-- 五、宝可梦形态关联表
-- ==========================================

-- 形态-属性关联表
DROP TABLE IF EXISTS pokemon_form_type;
CREATE TABLE pokemon_form_type (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    form_id INTEGER NOT NULL,
    type_id INTEGER NOT NULL,
    slot INTEGER NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (form_id) REFERENCES pokemon_form(id) ON DELETE CASCADE,
    FOREIGN KEY (type_id) REFERENCES type(id),
    UNIQUE (form_id, slot)
);

-- 形态-特性关联表
DROP TABLE IF EXISTS pokemon_form_ability;
CREATE TABLE pokemon_form_ability (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    form_id INTEGER NOT NULL,
    ability_id INTEGER NOT NULL,
    is_hidden INTEGER DEFAULT 0,
    slot INTEGER NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (form_id) REFERENCES pokemon_form(id) ON DELETE CASCADE,
    FOREIGN KEY (ability_id) REFERENCES ability(id),
    UNIQUE (form_id, slot)
);

-- 形态种族值表
DROP TABLE IF EXISTS pokemon_form_stat;
CREATE TABLE pokemon_form_stat (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    form_id INTEGER NOT NULL,
    stat_id INTEGER NOT NULL,
    base_stat INTEGER NOT NULL,
    effort INTEGER DEFAULT 0,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (form_id) REFERENCES pokemon_form(id) ON DELETE CASCADE,
    FOREIGN KEY (stat_id) REFERENCES stat(id),
    UNIQUE (form_id, stat_id)
);

-- 物种-蛋群关联表
DROP TABLE IF EXISTS pokemon_species_egg_group;
CREATE TABLE pokemon_species_egg_group (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    species_id INTEGER NOT NULL,
    egg_group_id INTEGER NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (species_id) REFERENCES pokemon_species(id) ON DELETE CASCADE,
    FOREIGN KEY (egg_group_id) REFERENCES egg_group(id),
    UNIQUE (species_id, egg_group_id)
);

-- ==========================================
-- 六、进化详情表
-- ==========================================

-- 进化详情表
DROP TABLE IF EXISTS pokemon_evolution;
CREATE TABLE pokemon_evolution (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    evolved_species_id INTEGER NOT NULL,
    evolves_from_species_id INTEGER,
    evolution_trigger_id INTEGER NOT NULL,
    min_level INTEGER,
    min_happiness INTEGER,
    min_affection INTEGER,
    min_beauty INTEGER,
    time_of_day TEXT,
    held_item_id INTEGER,
    evolution_item_id INTEGER,
    known_move_id INTEGER,
    known_move_type_id INTEGER,
    location_id INTEGER,
    party_species_id INTEGER,
    party_type_id INTEGER,
    trade_species_id INTEGER,
    needs_overworld_rain INTEGER DEFAULT 0,
    needs_multiplayer INTEGER DEFAULT 0,
    turn_upside_down INTEGER DEFAULT 0,
    relative_physical_stats INTEGER,
    gender_id INTEGER,
    region_id INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (evolved_species_id) REFERENCES pokemon_species(id),
    FOREIGN KEY (evolves_from_species_id) REFERENCES pokemon_species(id),
    FOREIGN KEY (evolution_trigger_id) REFERENCES evolution_trigger(id),
    FOREIGN KEY (held_item_id) REFERENCES item(id),
    FOREIGN KEY (evolution_item_id) REFERENCES item(id),
    FOREIGN KEY (known_move_id) REFERENCES move(id),
    FOREIGN KEY (known_move_type_id) REFERENCES type(id)
);

-- ==========================================
-- 七、技能学习表
-- ==========================================

-- 形态可学技能表
DROP TABLE IF EXISTS pokemon_form_move;
CREATE TABLE pokemon_form_move (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    form_id INTEGER NOT NULL,
    move_id INTEGER NOT NULL,
    learn_method_id INTEGER NOT NULL,
    level INTEGER,
    version_group_id INTEGER,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (form_id) REFERENCES pokemon_form(id) ON DELETE CASCADE,
    FOREIGN KEY (move_id) REFERENCES move(id),
    FOREIGN KEY (learn_method_id) REFERENCES move_learn_method(id),
    FOREIGN KEY (version_group_id) REFERENCES version_group(id)
);

-- ==========================================
-- 八、对战队伍表
-- ==========================================

-- 队伍表
DROP TABLE IF EXISTS battle_team;
CREATE TABLE battle_team (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    name TEXT,
    "format" TEXT,
    description TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 队伍成员表 (具体宝可梦实例)
DROP TABLE IF EXISTS battle_team_member;
CREATE TABLE battle_team_member (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    team_id INTEGER NOT NULL,
    form_id INTEGER NOT NULL,
    nickname TEXT,
    level INTEGER DEFAULT 50,
    gender TEXT,
    shiny INTEGER DEFAULT 0,
    nature_id INTEGER,
    ability_id INTEGER,
    held_item_id INTEGER,
    tera_type_id INTEGER,
    iv_hp INTEGER DEFAULT 31,
    iv_attack INTEGER DEFAULT 31,
    iv_defense INTEGER DEFAULT 31,
    iv_sp_attack INTEGER DEFAULT 31,
    iv_sp_defense INTEGER DEFAULT 31,
    iv_speed INTEGER DEFAULT 31,
    ev_hp INTEGER DEFAULT 0,
    ev_attack INTEGER DEFAULT 0,
    ev_defense INTEGER DEFAULT 0,
    ev_sp_attack INTEGER DEFAULT 0,
    ev_sp_defense INTEGER DEFAULT 0,
    ev_speed INTEGER DEFAULT 0,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (team_id) REFERENCES battle_team(id) ON DELETE CASCADE,
    FOREIGN KEY (form_id) REFERENCES pokemon_form(id),
    FOREIGN KEY (nature_id) REFERENCES nature(id),
    FOREIGN KEY (ability_id) REFERENCES ability(id),
    FOREIGN KEY (held_item_id) REFERENCES item(id),
    FOREIGN KEY (tera_type_id) REFERENCES type(id)
);

-- 队伍成员技能表
DROP TABLE IF EXISTS battle_team_member_move;
CREATE TABLE battle_team_member_move (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id INTEGER NOT NULL,
    move_id INTEGER NOT NULL,
    slot INTEGER NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES battle_team_member(id) ON DELETE CASCADE,
    FOREIGN KEY (move_id) REFERENCES move(id),
    UNIQUE (member_id, slot)
);

PRAGMA foreign_keys = ON;

-- ==========================================
-- 九、基础数据初始化
-- ==========================================

-- 世代数据
INSERT INTO generation (id, name, name_en, region, release_year) VALUES
(1, '第一世代', 'generation-i', 'kanto', 1996),
(2, '第二世代', 'generation-ii', 'johto', 1999),
(3, '第三世代', 'generation-iii', 'hoenn', 2002),
(4, '第四世代', 'generation-iv', 'sinnoh', 2006),
(5, '第五世代', 'generation-v', 'unova', 2010),
(6, '第六世代', 'generation-vi', 'kalos', 2013),
(7, '第七世代', 'generation-vii', 'alola', 2016),
(8, '第八世代', 'generation-viii', 'galar', 2019),
(9, '第九世代', 'generation-ix', 'paldea', 2022);

-- 属性数据
INSERT INTO type (id, name, name_en, name_jp, color) VALUES
(1, '一般', 'normal', 'ノーマル', '#A8A878'),
(2, '格斗', 'fighting', 'かくとう', '#C03028'),
(3, '飞行', 'flying', 'ひこう', '#A890F0'),
(4, '毒', 'poison', 'どく', '#A040A0'),
(5, '地面', 'ground', 'じめん', '#E0C068'),
(6, '岩石', 'rock', 'いわ', '#B8A038'),
(7, '虫', 'bug', 'むし', '#A8B820'),
(8, '幽灵', 'ghost', 'ゴースト', '#705898'),
(9, '钢', 'steel', 'はがね', '#B8B8D0'),
(10, '火', 'fire', 'ほのお', '#F08030'),
(11, '水', 'water', 'みず', '#6890F0'),
(12, '草', 'grass', 'くさ', '#78C850'),
(13, '电', 'electric', 'でんき', '#F8D030'),
(14, '超能力', 'psychic', 'エスパー', '#F85888'),
(15, '冰', 'ice', 'こおり', '#98D8D8'),
(16, '龙', 'dragon', 'ドラゴン', '#7038F8'),
(17, '恶', 'dark', 'あく', '#705848'),
(18, '妖精', 'fairy', 'フェアリー', '#EE99AC');

-- 种族值类型
INSERT INTO stat (id, name, name_en, name_jp, is_battle_only, game_index) VALUES
(1, 'HP', 'hp', 'HP', 0, 1),
(2, '攻击', 'attack', 'こうげき', 0, 2),
(3, '防御', 'defense', 'ぼうぎょ', 0, 3),
(4, '特攻', 'special-attack', 'とくこう', 0, 4),
(5, '特防', 'special-defense', 'とくぼう', 0, 5),
(6, '速度', 'speed', 'すばやさ', 0, 6),
(7, '命中', 'accuracy', 'めいちゅう', 1, 7),
(8, '闪避', 'evasion', 'かいひ', 1, 8);

-- 技能伤害类型
INSERT INTO move_damage_class (id, name, name_en, description) VALUES
(1, '物理', 'physical', '物理攻击技能'),
(2, '特殊', 'special', '特殊攻击技能'),
(3, '变化', 'status', '不造成直接伤害的技能');

-- 技能目标类型
INSERT INTO move_target (id, name, name_en, description) VALUES
(1, '单体', 'selected-pokemon', '指定单目标'),
(2, '对手全体', 'all-opponents', '所有对手'),
(3, '队友', 'user', '自身'),
(4, '除自身外所有', 'all-other-pokemon', '场上除自身外的所有宝可梦'),
(5, '邻接对手', 'adjacent-opponents', '邻接的对手'),
(10, '全体', 'all-pokemon', '场上所有宝可梦');

-- 进化触发条件
INSERT INTO evolution_trigger (id, name, name_en) VALUES
(1, '升级', 'level-up'),
(2, '交换', 'trade'),
(3, '使用物品', 'use-item'),
(4, '脱战', 'shed'),
(5, '招式', 'move'),
(6, '其他', 'other'),
(7, '等级提升', 'level-up-growth'),
(8, '好感度', 'affection'),
(9, '物理/特殊招式', 'physical-contact'),
(10, '携带特定道具', 'three-critical-hits'),
(11, '位置', 'takes-damage'),
(12, '时间', 'time'),
(13, '特定宝可梦在队伍中', 'turn-upside-down');

-- 创建索引以提高查询性能
CREATE INDEX idx_pokemon_form_type_form_id ON pokemon_form_type(form_id);
CREATE INDEX idx_pokemon_form_ability_form_id ON pokemon_form_ability(form_id);
CREATE INDEX idx_pokemon_form_stat_form_id ON pokemon_form_stat(form_id);
CREATE INDEX idx_pokemon_form_move_form_id ON pokemon_form_move(form_id);
CREATE INDEX idx_pokemon_form_move_move_id ON pokemon_form_move(move_id);
CREATE INDEX idx_pokemon_evolution_evolved ON pokemon_evolution(evolved_species_id);
CREATE INDEX idx_pokemon_evolution_from ON pokemon_evolution(evolves_from_species_id);
CREATE INDEX idx_move_flag_map_move_id ON move_flag_map(move_id);
CREATE INDEX idx_move_meta_move_id ON move_meta(move_id);
CREATE INDEX idx_move_meta_stat_change_move_id ON move_meta_stat_change(move_id);