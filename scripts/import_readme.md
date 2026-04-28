# Pokemon Factory 初始化脚本

数据库初始化和 CSV 导入已经收敛到 common 模块的 Java 初始化链，`scripts/` 目录只保留少量编排和校验脚本。

## 当前保留文件

```text
scripts/
├── init_db.py          # 删库 -> 构建/启动 common exec jar -> 等待建表与 CSV 导入 -> 校验
├── start-backend.py    # 跨平台启动 common exec jar，手动观察初始化日志
├── verify_sqlite.py    # 校验 SQLite 关键表和核心数据量
└── start-backend.ps1   # Windows 包装脚本，内部转调 start-backend.py
```

## 快速开始

```bash
# 一键重建数据库并导入 CSV
python scripts/init_db.py

# 跨平台手动启动 common，观察初始化日志
python scripts/start-backend.py

# 校验当前 SQLite 数据
python scripts/verify_sqlite.py
```

## 注意事项

- SQLite 默认路径为 `pokemon-factory-backend/pokemon-factory.db`
- common 默认可执行产物为 `pokemon-factory-backend/common/target/common-0.0.1-SNAPSHOT-exec.jar`
- **CSV 来源**：common 会从 `REMOTE_CSV_BASE_URL`（默认指向 PokeAPI CSV 源）按需下载 CSV 到本地缓存目录后再导入
- **缓存目录**：可通过 `CSV_CACHE_DIR` 环境变量自定义，默认为系统临时目录下的 `pokemon-factory/csv-cache`
- Windows 下仍可运行 `scripts/start-backend.ps1`，但推荐统一使用 `python scripts/start-backend.py`
- 历史上的 MySQL / Python 导入脚本已经移除，避免与 Java 初始化链重复维护
