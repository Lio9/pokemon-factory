Database notes

- 数据库相关脚本已统一收敛到 `pokemon-factory-backend/common/src/main/resources/db/init/`。
- 启动 `common` 模块时会自动初始化核心公共表、对战表和用户表；业务模块不再各自执行 Flyway。
- SQLite 已启用 WAL 模式，支持多模块并发读写。

How to initialize database:

1. Build `pokemon-factory-backend/common`
2. Run `java -jar pokemon-factory-backend/common/target/common-0.0.1-SNAPSHOT.jar`

JWT and runtime notes:
- Set `JWT_SECRET` env var for persistent tokens in production.
