# Pokemon Factory 数据导入脚本

将 PokeAPI CSV 数据导入到 SQLite 数据库。

## 目录结构

```
scripts/
├── import_from_csv.py          # 从 CSV 文件导入数据（主脚本）
├── import_to_sqlite.py         # SQLite 导入工具
├── import_move_flags.py        # 导入技能标签
├── insert_ability_item_effects.py  # 导入特性/道具效果
├── create_tables.py            # 建表脚本
├── init_db.py                  # 数据库初始化
├── add_indexes.py              # 添加索引
├── check_data.py               # 数据校验
├── verify_move_flags.py        # 校验技能标签
├── verify_sqlite.py            # 校验 SQLite 数据
└── start-backend.ps1           # 后端启动脚本 (PowerShell)
```

## 数据源

CSV 文件位于项目根目录 `csv/`（已从 Git 跟踪中移除，需本地保留）。

## 快速开始

```bash
# 1. 从 CSV 导入基础数据
python scripts/import_from_csv.py

# 2. 导入技能标签
python scripts/import_move_flags.py

# 3. 导入特性/道具效果
python scripts/insert_ability_item_effects.py

# 4. 校验数据完整性
python scripts/check_data.py
python scripts/verify_sqlite.py
```

## 注意事项

- 项目已切换到 SQLite，数据库文件由后端 `CommonDatabaseInitializer` 自动创建
- CSV 文件约 35MB，不纳入 Git 版本管理
- 语言 ID 参考：12=简体中文, 4=繁体中文, 9=英文, 1=日文
