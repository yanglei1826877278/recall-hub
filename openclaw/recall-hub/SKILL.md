---
name: recall-hub
description: 记录、查询和管理用户的私人待办、日记、想法、备忘与提醒。
---

# RecallHub

RecallHub 是用户个人数据的长期可信来源。通过 `scripts/recallhub.mjs` 调用 REST API，不要自行保存副本。

## 必须调用的场景

- 用户明确说“记一下”“帮我保存”“写进日记”。
- 用户表达一项未来行动，创建 `TODO`。
- 用户明确要求在某个时间提醒，创建 `TODO + Reminder`。
- 用户描述当天已经发生的个人经历，并希望留下记录，创建 `DIARY`。
- 用户要求查找、完成、延后或取消 RecallHub 中的内容。

普通聊天和知识问答默认不保存。提醒时间含糊时先询问；“明天下午三点”已经足够明确。

## 内容保真规则

RecallHub 用于保存用户的原始记忆，不要把写入动作当成摘要任务。

- 创建 `DIARY` 时，`entry.content` 必须忠实保留用户对经历的完整叙述，包括时间顺序、人物对话、数字、产品名、技术细节、口语、重复和情绪表达。
- 禁止擅自总结、精简、润色、改写、纠错、翻译、补充结论或合并细节。不要把长日记压缩成几句话。
- 只可以删掉明确的操作前缀，例如“记一下日记”“帮我保存”“写进日记”；前缀之后的正文保持原样。
- 不确定某个词是否说错时照原文保存。例如用户说 `WhatsApp2`，不得改成 `WhatsApp3`。
- 长内容仍然创建一条完整的 `DIARY`，不要为了简短而截断，也不要擅自拆成多条。
- 只有用户明确要求“帮我整理”“润色”或“总结”时才可以改写；没有这些要求时一律按原话保存。
- `source.rawContent` 保存用户发送的完整原始消息，包含操作前缀，便于核对来源；`entry.content` 保存去掉操作前缀后的完整正文。

例如用户说：

```text
记一下日记，今天没睡好，早上四点肚子疼把我疼醒了。
```

应提交：

```json
{
  "entry": {
    "type": "DIARY",
    "content": "今天没睡好，早上四点肚子疼把我疼醒了。"
  },
  "source": {
    "sourceType": "OPENCLAW",
    "channel": "WECHAT",
    "rawContent": "记一下日记，今天没睡好，早上四点肚子疼把我疼醒了。"
  }
}
```

## 配置

推荐在 OpenClaw 所在主机创建：

```text
~/.config/recallhub/config.json
```

内容：

```json
{
  "url": "http://192.168.3.79:8080",
  "token": "rh_..."
}
```

文件权限必须限制为当前用户读取：

```bash
chmod 600 ~/.config/recallhub/config.json
```

也可以通过环境变量覆盖配置文件：

```bash
export RECALLHUB_URL="http://127.0.0.1:8080"
export RECALLHUB_TOKEN="rh_..."
```

`RECALLHUB_CONFIG` 可以指定其他配置文件路径。这里的 Token 是 RecallHub“设置 → 访问令牌”生成的 `rh_...` API Token，不是 OpenClaw Hook Token。

所有时间使用带时区的 ISO 8601，例如 `2026-09-21T15:00:00+08:00`。

## 命令

请求 JSON 通过标准输入传入，避免 shell 转义破坏内容：

```bash
echo '{"entry":{"type":"IDEA","content":"RecallHub 增加月度回顾"},"source":{"sourceType":"OPENCLAW","channel":"WECHAT","conversationId":"c1","messageId":"m1"}}' \
  | node scripts/recallhub.mjs capture --key "openclaw:wechat:c1:m1"
```

带提醒的待办：

```bash
echo '{"entry":{"type":"TODO","title":"交电费"},"reminder":{"remindAt":"2026-09-21T15:00:00+08:00"},"source":{"sourceType":"OPENCLAW","channel":"WECHAT"}}' \
  | node scripts/recallhub.mjs capture --key "openclaw:wechat:c1:m2"
```

批量整理语音聊天：

```bash
echo '{"entries":[{"type":"DIARY","content":"下午解决了登录问题。"},{"type":"TODO","title":"研究 Jev"}],"source":{"sourceType":"OPENCLAW","channel":"VOICE"}}' \
  | node scripts/recallhub.mjs batch --key "openclaw:voice:c2:m1"
```

查询及操作：

```bash
node scripts/recallhub.mjs search "Jev"
node scripts/recallhub.mjs today
node scripts/recallhub.mjs complete 123
node scripts/recallhub.mjs snooze 456 "2026-09-21T16:00:00+08:00"
node scripts/recallhub.mjs cancel-reminder 456
```

调用失败时向用户说明错误，不要假装已经保存或发送。
