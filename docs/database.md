# 数据库

目标数据库是 MySQL 5.7.23，统一使用 InnoDB、`utf8mb4` 和 `utf8mb4_unicode_ci`。结构由 `server/src/main/resources/db/migration` 下的 Flyway migration 管理。

全文搜索使用 `(title, content) WITH PARSER ngram`。服务器建议保持 `ngram_token_size=2`，修改后必须重建全文索引。

Reminder 不使用 MySQL 8 的 `SKIP LOCKED`。Worker 先读取候选 ID，再用带状态和时间条件的 UPDATE 抢占任务。

