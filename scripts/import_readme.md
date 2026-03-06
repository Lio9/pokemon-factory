# Pokemon Factory 数据导入使用教程

本教程介绍如何将 PokeAPI 的数据导入到 Pokemon Factory 数据库中。

## 目录结构

```
scripts/
├── import_from_csv.py      # 从CSV文件导入数据（推荐）
├── import_from_csv.sql     # MySQL LOAD DATA导入脚本
├── complete_import.py      # 从网络API导入数据
├── import_manager.py       # 导入管理器
├── pokemon_import.py       # 宝可梦导入
├── ability_import.py       # 特性导入
├── move_import.py          # 技能导入
├── type_import.py          # 属性导入
├── stats_import.py         # 种族值导入
├── egg_group_import.py     # 蛋群导入
├── form_import.py          # 形态导入
└── utils.py                # 工具函数
```

## 数据源

本项目使用 PokeAPI 的官方数据，CSV 文件位于：
```
pokeapi/data/v2/csv/
```

## 快速开始

### 方法一：从 CSV 文件导入（推荐）

这是最快的方式，直接从本地 CSV 文件导入数据：

```bash
python scripts/import_from_csv.py
```

**导入顺序：**
1. 属性 (types) → 2. 特性 (abilities) → 3. 技能 (moves) → 4. 蛋群 (egg_groups) → 5. 进化链 (evolution_chains) → 6. 物种 (pokemon_species) → 7. 宝可梦 (pokemon) → 8. 形态 (pokemon_forms) → 9. 属性关联 → 10. 特性关联 → 11. 种族值 → 12. 蛋群关联

**预计导入数据量：**

| 数据表 | 记录数 |
|--------|--------|
| type (属性) | 22 |
| ability (特性) | 310 |
| move (技能) | 939 |
| egg_group (蛋群) | 15 |
| evolution_chain (进化链) | 545 |
| pokemon_species (物种) | 1025 |
| pokemon (宝可梦) | 1025 |
| pokemon_form (形态) | 1025 |
| pokemon_form_type (属性关联) | ~2000 |
| pokemon_form_ability (特性关联) | ~2800 |
| pokemon_stats (种族值) | 1025 |
| pokemon_egg_group (蛋群关联) | ~1600 |

### 方法二：从网络 API 导入

如果需要最新数据或 CSV 文件不完整，可以从 PokeAPI 网络接口导入：

```bash
python scripts/complete_import.py
```

或使用导入管理器：

```bash
# 导入全部数据
python scripts/import_manager.py all

# 导入单个类型数据
python scripts/import_manager.py type       # 仅导入属性
python scripts/import_manager.py ability    # 仅导入特性
python scripts/import_manager.py move       # 仅导入技能
python scripts/import_manager.py pokemon    # 仅导入宝可梦
```

## 数据库配置

数据库配置在 `utils.py` 文件中：

```python
DB_CONFIG = {
    "host": "10.144.55.168",
    "port": 3306,
    "user": "root",
    "password": "your_password",
    "database": "pokemon_factory",
    "charset": "utf8mb4",
}
```

## 数据表结构

### 核心表

1. **type (属性表)**
   - id, name, name_en, name_jp, color

2. **ability (特性表)**
   - id, index_number, name, name_en, name_jp, description, effect

3. **move (技能表)**
   - id, index_number, name, name_en, type_id, power, pp, accuracy, damage_class

4. **pokemon (宝可梦表)**
   - id, index_number, name, name_en, height, weight, base_experience, species_id

5. **pokemon_species (物种表)**
   - id, name, evolution_chain_id, capture_rate, is_legendary, is_mythical

### 关联表

1. **pokemon_form_type** - 形态与属性关联
2. **pokemon_form_ability** - 形态与特性关联
3. **pokemon_stats** - 种族值数据
4. **pokemon_egg_group** - 宝可梦与蛋群关联

## 常见问题

### Q: 导入失败，提示数据库连接错误？

检查数据库配置是否正确，确保数据库服务已启动：
```bash
# 测试数据库连接
mysql -h 10.144.55.168 -u root -p pokemon_factory
```

### Q: CSV 文件不存在？

CSV 文件位于 `pokeapi/data/v2/csv/` 目录。如果目录为空，请确保已正确克隆 pokeapi 子模块：
```bash
git submodule update --init --recursive
```

### Q: 如何清空数据重新导入？

```sql
-- 在 MySQL 中执行
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE pokemon_egg_group;
TRUNCATE TABLE pokemon_stats;
TRUNCATE TABLE pokemon_form_ability;
TRUNCATE TABLE pokemon_form_type;
TRUNCATE TABLE pokemon_form;
TRUNCATE TABLE pokemon;
TRUNCATE TABLE pokemon_species;
TRUNCATE TABLE evolution_chain;
TRUNCATE TABLE egg_group;
TRUNCATE TABLE move;
TRUNCATE TABLE ability;
TRUNCATE TABLE type;
SET FOREIGN_KEY_CHECKS = 1;
```

### Q: 中文名称显示不正确？

CSV 文件中包含多语言名称数据：
- 语言 ID 12 = 简体中文
- 语言 ID 4 = 繁体中文
- 语言 ID 9 = 英文
- 语言 ID 1 = 日文

脚本会优先使用简体中文，其次是繁体中文、英文。

## API 数据结构参考

PokeAPI 的数据结构请参考：
- 模型定义：`pokeapi/pokemon_v2/models.py`
- CSV 数据：`pokeapi/data/v2/csv/`
- 官方文档：https://pokeapi.co/docs/v2

## 更新日志

- 2026-03-06: 创建 CSV 导入脚本，支持完整的 PokeAPI 数据导入
