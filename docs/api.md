# API

所有业务接口以 `/api/v1` 开头。网页使用 Session Cookie，OpenClaw 使用 `Authorization: Bearer rh_...`。

核心接口：

- `POST /captures`、`POST /captures/batch`
- `GET|POST /entries`、`GET|PATCH|DELETE /entries/{id}`
- `POST /entries/{id}/complete`
- `GET|POST /reminders`、`PATCH /reminders/{id}`
- `POST /reminders/{id}/cancel|snooze`
- `GET /today`、`GET /timeline`、`GET /search`
- `GET|PUT /journals/{date}`
- `GET|POST|PUT|DELETE /themes`
- `GET /export/json|markdown`

Capture 支持 `Idempotency-Key`，相同 Key 和相同请求会返回第一次结果；相同 Key 搭配不同请求返回冲突。

