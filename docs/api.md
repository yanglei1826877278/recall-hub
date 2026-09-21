# API

所有业务接口以 `/api/v1` 开头。网页使用 Session Cookie，OpenClaw 使用 `Authorization: Bearer rh_...`。

核心接口：

- `POST /captures`、`POST /captures/batch`
- `GET|POST /entries`、`GET|PATCH|DELETE /entries/{id}`
- `POST /entries/{id}/complete`
- `GET|POST /reminders`、`PATCH /reminders/{id}`
- `POST /reminders/{id}/cancel|snooze`
- `GET /today`、`GET /timeline`、`GET /search`
- `GET|PUT /journals/{date}`、`POST /journals/{date}/generate`
- `GET|POST|DELETE /journals/{date}/share`
- `PATCH /journals/{date}/share/password`、`PUT /journals/{date}/share/content`
- `GET /public/journal-shares/{token}`、`POST /public/journal-shares/{token}/unlock`
- `GET|POST|PUT|DELETE /themes`
- `GET /export/json|markdown`

Capture 支持 `Idempotency-Key`，相同 Key 和相同请求会返回第一次结果；相同 Key 搭配不同请求返回冲突。

日记 AI 整理使用设置中的 `ai_base_url`、`ai_api_key` 和 `ai_model` 调用兼容 OpenAI Responses API 的 `/responses` 接口。API Key 不会通过设置读取接口回显。

日记分享按单日创建正文快照，必须设置 4 位数字查看密码。公开接口只返回快照日期和正文；修改密码会使已有解锁会话失效，取消分享后原链接不可再次使用。
