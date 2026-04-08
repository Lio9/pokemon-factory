-- V2__seed_basic_data.sql - seed generations and types

-- Seed generations (使用 SQLite 的 UPSERT 语法)
INSERT INTO generation (id, name, name_en, region, release_year) VALUES
(1, '第一世代', 'generation-i', 'Kanto', 1996),
(2, '第二世代', 'generation-ii', 'Johto', 1999),
(3, '第三世代', 'generation-iii', 'Hoenn', 2002),
(4, '第四世代', 'generation-iv', 'Sinnoh', 2006),
(5, '第五世代', 'generation-v', 'Unova', 2010),
(6, '第六世代', 'generation-vi', 'Kalos', 2013),
(7, '第七世代', 'generation-vii', 'Alola', 2016),
(8, '第八世代', 'generation-viii', 'Galar', 2019),
(9, '第九世代', 'generation-ix', 'Paldea', 2022)
ON CONFLICT(id) DO UPDATE SET
  name=excluded.name,
  name_en=excluded.name_en,
  region=excluded.region,
  release_year=excluded.release_year;

-- Seed types (使用 SQLite 兼容语法)
INSERT INTO type (id, name, name_en, color) VALUES
(1, '一般', 'normal', '#A8A878'),
(2, '格斗', 'fighting', '#C03028'),
(3, '飞行', 'flying', '#A890F0'),
(4, '毒', 'poison', '#A040A0'),
(5, '地面', 'ground', '#E0C068'),
(6, '岩石', 'rock', '#B8A038'),
(7, '虫', 'bug', '#A8B820'),
(8, '幽灵', 'ghost', '#705898'),
(9, '钢', 'steel', '#B8B8D0'),
(10, '火', 'fire', '#F08030'),
(11, '水', 'water', '#6890F0'),
(12, '草', 'grass', '#78C850'),
(13, '电', 'electric', '#F8D030'),
(14, '超能', 'psychic', '#F85888'),
(15, '冰', 'ice', '#98D8D8'),
(16, '龙', 'dragon', '#7038F8'),
(17, '恶', 'dark', '#705848'),
(18, '妖精', 'fairy', '#EE99AC')
ON CONFLICT(id) DO UPDATE SET
  name=excluded.name,
  name_en=excluded.name_en,
  color=excluded.color;
