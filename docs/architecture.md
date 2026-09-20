# 架构

RecallHub 将交互、长期状态和投递职责拆开：OpenClaw 负责自然语言交互，RecallHub 负责业务与调度，MySQL 是唯一可信数据源。

`Capture API` 在一个事务内创建 Source、Entry 和可选 Reminder。Reminder Worker 每 10 秒扫描到期任务，通过条件更新获取两分钟租约，成功后调用 OpenClaw `/hooks/agent`。真实 Hook 响应 `{"ok":true,"runId":"..."}` 被视为成功；如果响应提供 `completion.status` 或 `delivered`，也必须通过相应检查。

数据库时间使用 UTC `DATETIME(3)`，API 使用带时区的 ISO 8601。界面日期按 `RECALLHUB_TIMEZONE` 解释。

