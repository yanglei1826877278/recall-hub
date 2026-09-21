# RecallHub

<img src="web/public/logo.png" alt="RecallHub Logo" width="128" />

RecallHub 是运行在个人电脑上的单用户私人信息系统，用来保存和查找：

- 待办与完成状态
- 提醒与延后提醒
- 日记及每日整理内容
- 想法与备忘
- OpenClaw 从微信、Telegram、QQ 或语音聊天中提取的有效内容
- 自定义明暗主题
- JSON、Markdown 和主题导出

OpenClaw 负责交流，RecallHub 负责长期保存和提醒调度，MySQL 是唯一可信数据源。

```text
微信 / Telegram / QQ
          ↓
      OpenClaw
          ↓ RecallHub Skill + API Token
      RecallHub API
          ↓
        MySQL

到期提醒：MySQL → RecallHub Worker → OpenClaw Hook → 微信/TG/QQ
```

---

## 1. 项目目录

```text
recall-hub/
├── server/                  Spring Boot 后端
├── web/                     Vue 3 前端
├── openclaw/recall-hub/     OpenClaw Skill
├── deploy/                  构建脚本与 systemd 服务
├── docs/                    架构和接口补充文档
├── .env.example             配置模板
└── README.md
```

技术基线：

- Java 17
- Spring Boot 3.5
- Maven
- MyBatis-Plus
- Flyway
- MySQL 5.7.23
- Vue 3 + TypeScript + Vite + Pinia
- Node.js 22 + pnpm

---

## 2. 运行前准备

开发电脑需要：

```text
JDK 17
Maven 3.8+
MySQL 5.7.23
Node.js 22
pnpm 9+
```

检查版本：

```powershell
java -version
mvn -version
node -v
pnpm -v
mysql --version
```

默认端口：

| 服务 | 端口 |
|---|---:|
| RecallHub 后端 | `8080` |
| Vite 开发服务器 | `5173` |
| MySQL | `3306` |
| OpenClaw Gateway | `18789` |

---

## 3. 创建 MySQL 数据库

使用有管理权限的 MySQL 账户登录：

```powershell
mysql -uroot -p
```

执行以下 SQL，并将密码换成自己的密码：

```sql
CREATE DATABASE recall_hub
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'recallhub'@'localhost'
  IDENTIFIED BY '替换成强密码';

GRANT ALL PRIVILEGES ON recall_hub.*
  TO 'recallhub'@'localhost';

FLUSH PRIVILEGES;
```

如果数据库和 RecallHub 不在同一台电脑，需要根据实际网络调整 MySQL 用户允许的 Host，并限制防火墙来源。不要把 MySQL `3306` 直接开放到公网。

数据库表不需要手工创建。后端第一次启动时，Flyway 会自动执行 `server/src/main/resources/db/migration/` 中的全部迁移。

---

## 4. 配置 RecallHub

在项目根目录操作：

```powershell
cd "I:\JAVA\IDEA代码\recall-hub"
Copy-Item .env.example .env
notepad .env
```

填写 `.env`：

```properties
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=recall_hub
MYSQL_USERNAME=recallhub
MYSQL_PASSWORD=替换成数据库密码

SERVER_PORT=8080
RECALLHUB_TIMEZONE=Asia/Shanghai

# 可以先留空，启动后在“设置 → OpenClaw”中填写
OPENCLAW_BASE_URL=http://127.0.0.1:18789
OPENCLAW_HOOK_TOKEN=

BACKUP_PATH=./backups
MYSQLDUMP_PATH=mysqldump
BACKUP_ENABLED=false
```

如果已经使用了其他数据库名（例如 `recallhub`），将 `MYSQL_DATABASE` 改成现有名称即可。

Spring Boot 会自动寻找：

- 从项目根目录启动时的 `./.env`
- 从 `server/` 启动时的 `../.env`

`.env` 已被 `.gitignore` 排除，不能提交到仓库。

### 使用 IntelliJ IDEA

直接运行：

```text
server/src/main/java/com/recallhub/RecallHubApplication.java
```

将 Run/Debug Configuration 的 Working directory 设置为：

```text
I:\JAVA\IDEA代码\recall-hub\server
```

也可以在 Environment variables 中直接填写 `.env` 里的变量。

---

## 5. 启动后端

在 Windows PowerShell 执行：

```powershell
cd "I:\JAVA\IDEA代码\recall-hub\server"
mvn spring-boot:run
```

检查健康状态：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
```

期望结果：

```json
{
  "status": "UP"
}
```

每次新增 Flyway migration 后都需要重启后端，让迁移自动执行。

---

## 6. 启动前端

打开另一个 Windows PowerShell：

```powershell
cd "I:\JAVA\IDEA代码\recall-hub\web"
pnpm install
pnpm dev
```

访问：

```text
http://127.0.0.1:5173
```

Vite 会把 `/api` 和 `/actuator` 代理到 `http://127.0.0.1:8080`。

首次打开时会进入初始化页面：

1. 输入本地用户名。
2. 设置至少 8 位密码。
3. 创建后自动登录。

该账户只用于 RecallHub 网页，不是 MySQL 账户，也不是 OpenClaw 账户。

---

## 7. 基本使用

### 今天

- 快速创建待办、日记、想法和备忘。
- 可同时填写截止时间和提醒时间。
- 查看超时任务、今日提醒、待办和今日记录。

### 时间线

- 按时间倒序查看全部内容。
- 按日记、待办、想法、备忘筛选。

### 日记

- 查看每日原始日记片段。
- 手工编辑当天整理后的日记。
- 原始片段不会被整理后的内容覆盖。
- 可为单独一天创建带 4 位查看密码的分享链接；分享内容使用独立快照，可随时更新快照、修改密码或取消分享。

### 搜索

- 使用 MySQL FULLTEXT + ngram 搜索中文和英文内容。
- 可按内容类型筛选。

### 设置

- 外观与主题：切换明暗模式，导入或导出 `globals.css`。
- OpenClaw：配置 Gateway URL、Hook Token 和提醒渠道。
- 访问令牌：生成供 OpenClaw Skill 使用的 `rh_...` Token。
- 备份与导出：导出 JSON、Markdown，或者手工执行数据库备份。

---

## 8. 配置 OpenClaw 提醒投递

进入 RecallHub：

```text
设置 → OpenClaw
```

### 8.1 Gateway Base URL

填写 RecallHub 能访问到的 OpenClaw Gateway 地址，例如：

```text
http://127.0.0.1:18789
http://192.168.3.79:18789
http://100.x.x.x:18789
https://openclaw.example.com
```

数据库中保存的地址优先于 `OPENCLAW_BASE_URL` 环境变量。留空时使用环境变量。

### 8.2 Hook Token

填写 OpenClaw `/hooks/agent` 使用的 Bearer Token。

- 数据库 Token 优先于 `OPENCLAW_HOOK_TOKEN` 环境变量。
- Token 保存后不会在页面或设置 API 中回显。
- 输入框留空并保存不会覆盖已有 Token。
- “清除数据库 Token”后重新使用环境变量。

### 8.3 提醒渠道

微信示例：

```text
名称：我的微信
Channel：openclaw-weixin
Target：用户目标地址，例如 ...@im.wechat
Account ID：OpenClaw 微信机器人账户 ID
Agent ID：main
默认渠道：是
```

`Target`、`Account ID`、Hook Token 都属于私密配置，不能写进代码仓库或提交到 Git。

### 8.4 两种 Token 的区别

| Token | 方向 | 用途 | 配置位置 |
|---|---|---|---|
| OpenClaw Hook Token | RecallHub → OpenClaw | 到期后调用 `/hooks/agent` 发消息 | 设置 → OpenClaw |
| RecallHub API Token（`rh_...`） | OpenClaw → RecallHub | Skill 写入和查询 RecallHub | 设置 → 访问令牌 |

不要把这两个 Token 混用。

---

## 9. 生成 RecallHub API Token

进入：

```text
设置 → 访问令牌
```

点击“生成 Token”。系统会创建带以下权限的 `rh_...` Token：

```text
ENTRY_READ
ENTRY_WRITE
REMINDER_READ
REMINDER_WRITE
SEARCH
JOURNAL_READ
JOURNAL_WRITE
```

明文 Token 只显示一次，请立即复制。数据库只保存 Token 的 SHA-256 哈希，之后无法恢复明文；遗失后应撤销旧 Token 并重新生成。

---

## 10. 通过 SSH 安装 OpenClaw Skill

当前场景：

```text
RecallHub 项目：Windows 本机
OpenClaw 主机：menghanjun@ai-agent
OpenClaw Skill 目标目录：
/home/menghanjun/.openclaw/workspace/skills/
```

以下命令在 **Windows PowerShell** 中执行。

### 10.1 创建远程技能目录

```powershell
ssh menghanjun@ai-agent `
  "mkdir -p /home/menghanjun/.openclaw/workspace/skills"
```

### 10.2 上传 Skill

```powershell
scp -r `
  "I:\JAVA\IDEA代码\recall-hub\openclaw\recall-hub" `
  "menghanjun@ai-agent:/home/menghanjun/.openclaw/workspace/skills/"
```

目标结构应为：

```text
/home/menghanjun/.openclaw/workspace/skills/recall-hub/
├── SKILL.md
└── scripts/
    └── recallhub.mjs
```

设置 Helper 权限并检查技能：

```powershell
ssh -t menghanjun@ai-agent `
  'export PATH="/home/menghanjun/.nvm/versions/node/v22.23.1/bin:$PATH"; chmod 755 /home/menghanjun/.openclaw/workspace/skills/recall-hub/scripts/recallhub.mjs; openclaw skills info recall-hub; openclaw skills check'
```

如果当前会话没有刷新技能列表：

```powershell
ssh -t menghanjun@ai-agent `
  'export PATH="/home/menghanjun/.nvm/versions/node/v22.23.1/bin:$PATH"; openclaw gateway restart'
```

然后新开一个 OpenClaw 会话。

### 10.3 更新 Skill

项目里的 Skill 修改后，重新执行上传命令即可。

---

## 11. 在远程 OpenClaw 主机配置 Skill

先登录远程主机：

```powershell
ssh -t menghanjun@ai-agent
```

下面的命令在 `ai-agent` 的 Linux Shell 中执行。

### 11.1 创建配置目录

```bash
mkdir -p /home/menghanjun/.config/recallhub
chmod 700 /home/menghanjun/.config/recallhub
```

### 11.2 创建配置文件

```bash
nano /home/menghanjun/.config/recallhub/config.json
```

填写：

```json
{
  "url": "http://192.168.3.79:8080",
  "token": "rh_这里填写访问令牌"
}
```

这里的 URL 是 **RecallHub 后端地址**，端口通常是 `8080`。不要填写 OpenClaw 的 `18789`。

限制文件权限：

```bash
chmod 600 /home/menghanjun/.config/recallhub/config.json
```

Helper 每次执行都会重新读取配置文件，因此修改该文件后不需要重启 OpenClaw。

### 11.3 环境变量方式

环境变量优先于配置文件：

```bash
export RECALLHUB_URL="http://192.168.3.79:8080"
export RECALLHUB_TOKEN="rh_这里填写访问令牌"
```

也可以用 `RECALLHUB_CONFIG` 指定其他配置文件：

```bash
export RECALLHUB_CONFIG="/path/to/config.json"
```

如果通过 systemd 启动 OpenClaw，普通终端里的 `export` 不一定会传给 Gateway，推荐使用 `config.json`。

---

## 12. 验证 Skill

在 `ai-agent` 中执行：

```bash
cd /home/menghanjun/.openclaw/workspace/skills/recall-hub

/home/menghanjun/.nvm/versions/node/v22.23.1/bin/node \
  scripts/recallhub.mjs today
```

如果返回包含 `date`、`todos`、`entries` 等字段的 JSON，说明 URL 和 Token 配置成功。

搜索测试：

```bash
/home/menghanjun/.nvm/versions/node/v22.23.1/bin/node \
  scripts/recallhub.mjs search "RecallHub"
```

在 OpenClaw 新会话中测试：

```text
$recall-hub 帮我记一下，测试 RecallHub 技能
```

也可以直接说：

```text
帮我记一下，测试 RecallHub 技能
```

网页中出现对应记录即表示完整写入链路正常：

```text
OpenClaw → RecallHub Skill → Capture API → MySQL → RecallHub Web
```

---

## 13. 测试提醒链路

在 OpenClaw 中发送：

```text
两分钟后提醒我测试 RecallHub
```

预期结果：

1. RecallHub 中创建一条 `TODO`。
2. 同时创建一条 `Reminder`。
3. Reminder Worker 到期后调用 OpenClaw `/hooks/agent`。
4. 微信、Telegram 或 QQ 收到提醒。
5. Reminder 状态变为 `SENT`。

真实 OpenClaw Hook 成功响应可能只有：

```json
{
  "ok": true,
  "runId": "64755483-3b4b-4421-bd95-a2a39d7ed58d"
}
```

RecallHub 同时兼容响应中存在 `completion.status`、`deliveryAttempted` 和 `delivered` 的情况。

失败时会按照以下节奏重试：

```text
1 分钟 → 5 分钟 → 15 分钟 → FAILED
```

---

## 14. 主题导入

进入：

```text
设置 → 外观与主题 → 导入 globals.css
```

允许格式：

```css
:root {
  --background: oklch(1 0 0);
  --foreground: oklch(0.2 0 0);
  --primary: oklch(0.56 0.16 250);
  --radius: 1rem;
}

.dark {
  --background: oklch(0.17 0.02 255);
  --foreground: oklch(0.95 0.01 245);
  --primary: oklch(0.72 0.13 245);
}
```

导入器只接受 `:root` 和 `.dark` 中允许的 CSS Variables，会拒绝其他选择器、`@import`、`url()` 和 `expression()`。

---

## 15. 数据导出与备份

### 数据导出

进入：

```text
设置 → 备份与导出
```

支持：

- JSON：完整结构化数据。
- Markdown ZIP：日记、待办、想法和备忘。
- `globals.css`：单独从主题列表导出。

### 自动数据库备份

在 `.env` 中启用：

```properties
BACKUP_ENABLED=true
BACKUP_PATH=./backups
MYSQLDUMP_PATH=mysqldump
```

如果 `mysqldump` 不在 PATH，填写完整路径，例如：

```properties
MYSQLDUMP_PATH=I:/MySQL/mysql-5.7.23-winx64/bin/mysqldump.exe
```

自动备份默认每天 `02:15` 执行，保留：

```text
最近 7 个每日备份
最近 4 个每周备份
最近 6 个每月备份
```

也可以在设置页手工点击“立即备份”。

---

## 16. 生产构建

### Windows 手工构建 WAR

在 Windows PowerShell 中执行：

```powershell
cd "I:\JAVA\IDEA代码\recall-hub\web"

pnpm install --frozen-lockfile
pnpm build

cd ..

$StaticPath = "I:\JAVA\IDEA代码\recall-hub\server\src\main\resources\static"

if (Test-Path -LiteralPath $StaticPath) {
    Remove-Item -LiteralPath $StaticPath -Recurse -Force
}

New-Item -ItemType Directory -Path $StaticPath | Out-Null
Copy-Item -Path ".\web\dist\*" -Destination $StaticPath -Recurse

cd server
mvn package -DskipTests
```

需要完全清理后重新构建时，将最后一条命令改成：

```powershell
mvn clean package -DskipTests
```

### Linux 手工构建 WAR

在 Linux 项目根目录执行：

```bash
cd /path/to/recall-hub/web

pnpm install --frozen-lockfile
pnpm build

cd ..

rm -rf server/src/main/resources/static
mkdir -p server/src/main/resources/static
cp -a web/dist/. server/src/main/resources/static/

cd server
mvn package -DskipTests
```

需要完全清理后重新构建：

```bash
mvn clean package -DskipTests
```

如果只修改了 Java，并且 `server/src/main/resources/static` 中已经包含正确的前端资源，可以直接执行 Maven 打包，不必重新构建前端。

最终 WAR：

```text
server/target/recall-hub-server-1.0.0.war
```

该 WAR 仍然可以不依赖外部 Tomcat，直接运行：

```powershell
cd "I:\JAVA\IDEA代码\recall-hub"
java -jar server\target\recall-hub-server-1.0.0.war --spring.profiles.active=prod
```

生产环境只需访问：

```text
http://RecallHub主机地址:8080
```

前端已经包含在 WAR 中，不需要再运行 Vite。

Linux 常驻服务示例位于 `deploy/recallhub.service`。

### Docker 多阶段自动构建（可选，适合 CI）

仓库已提供：

```text
deploy/tomcat/Dockerfile
docker-compose.yml
.env.docker.example
.dockerignore
```

Dockerfile 使用三阶段构建：

```text
Node.js 22 构建 Vue
        ↓
Maven + JDK 17 构建 WAR
        ↓
Tomcat 10.1 + JDK 17 运行 ROOT.war
```

最终运行镜像基于：

```text
tomcat:10.1.60-jdk17-temurin-noble
```

#### 1. 准备容器配置

在项目根目录执行：

```powershell
cd "I:\JAVA\IDEA代码\recall-hub"
Copy-Item .env.docker.example .env.docker
notepad .env.docker
```

Docker Desktop 中的 RecallHub 容器访问 Windows 宿主机 MySQL 时，使用：

```properties
MYSQL_HOST=host.docker.internal
MYSQL_PORT=3306
MYSQL_DATABASE=recall_hub
MYSQL_USERNAME=recallhub
MYSQL_PASSWORD=替换成数据库密码
RECALLHUB_TIMEZONE=Asia/Shanghai
# OPENCLAW_BASE_URL=http://OpenClaw主机:18789
# OPENCLAW_HOOK_TOKEN=替换成HookToken
BACKUP_ENABLED=false
```

`.env.docker` 已被 Git 忽略，不能提交。

#### 2. 允许容器连接宿主机 MySQL

MySQL 中只有 `'recallhub'@'localhost'` 时，Docker 容器无法使用该账户。为容器连接创建账户：

```sql
CREATE USER 'recallhub'@'%'
  IDENTIFIED BY '与 .env.docker 相同的密码';

GRANT ALL PRIVILEGES ON recall_hub.*
  TO 'recallhub'@'%';

FLUSH PRIVILEGES;
```

同时确认 MySQL 监听地址允许 Docker 网络访问，并用 Windows 防火墙限制 `3306` 的来源。不要把 `3306` 开放到公网。

如果 MySQL 也运行在同一个 Compose 项目中，`MYSQL_HOST` 应填写 MySQL 的 Compose 服务名，而不是 `host.docker.internal`。

#### 3. 构建并启动

```powershell
docker compose build --pull
docker compose up -d
```

查看状态：

```powershell
docker compose ps
docker compose logs -f recallhub
```

容器启动时会自动执行 Flyway migration。健康状态变为 `healthy` 后访问：

```text
http://127.0.0.1:8080/
http://127.0.0.1:8080/actuator/health
```

如需修改宿主机端口，在项目根目录 `.env` 中设置：

```properties
RECALLHUB_HTTP_PORT=8090
```

随后访问 `http://127.0.0.1:8090/`。容器内部仍使用 Tomcat `8080`。

#### 4. 配置 OpenClaw

容器启动后进入：

```text
设置 → OpenClaw
```

填写容器能够访问的 OpenClaw Gateway 地址与 Hook Token。如果 OpenClaw 在另一台机器，应使用对应的局域网、Tailscale 或 HTTPS 地址。

远程 OpenClaw Skill 中的 RecallHub URL 应填写 Docker 宿主机地址，例如：

```json
{
  "url": "http://192.168.3.79:8080",
  "token": "rh_..."
}
```

#### 5. 数据与备份

RecallHub 业务数据保存在外部 MySQL，不会因为重建 Tomcat 容器而消失。

Compose 将宿主机 `./backups` 挂载到容器 `/opt/recallhub/backups`。基础镜像没有安装 `mysqldump`，因此 Docker 部署默认 `BACKUP_ENABLED=false`。建议由 MySQL 容器或宿主机执行数据库备份；JSON 和 Markdown 导出仍可在网页中使用。

#### 6. 更新镜像

代码更新后执行：

```powershell
docker compose build --pull
docker compose up -d --force-recreate
docker compose logs -f recallhub
```

停止服务：

```powershell
docker compose down
```

该命令只停止并删除 RecallHub 容器和网络，不会删除外部 MySQL 数据。

### Docker Tomcat 部署预构建 WAR（推荐）

先按照前面的 Windows 或 Linux 步骤生成：

```text
server/target/recall-hub-server-1.0.0.war
```

`docker-compose.prebuilt.yml` 不执行 Node、pnpm、Maven 或镜像构建，只把现有 WAR 挂载到官方 Tomcat：

```text
宿主机：server/target/recall-hub-server-1.0.0.war
容器内：/usr/local/tomcat/webapps/ROOT.war
```

#### Windows Docker Desktop

准备配置：

```powershell
cd "I:\JAVA\IDEA代码\recall-hub"
Copy-Item .env.docker.example .env.docker
notepad .env.docker
```

宿主机 MySQL 地址填写：

```properties
MYSQL_HOST=host.docker.internal
```

启动：

```powershell
docker compose -f docker-compose.prebuilt.yml pull
docker compose -f docker-compose.prebuilt.yml up -d
docker compose -f docker-compose.prebuilt.yml ps
docker compose -f docker-compose.prebuilt.yml logs -f recallhub
```

#### Linux Docker

进入项目目录并准备配置：

```bash
cd /path/to/recall-hub
cp .env.docker.example .env.docker
nano .env.docker
```

如果 MySQL 运行在 Docker 宿主机，保留：

```properties
MYSQL_HOST=host.docker.internal
```

Compose 已通过以下配置将该主机名映射到 Linux Docker Host Gateway：

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

如果 MySQL 是另一个容器，`MYSQL_HOST` 应填写 MySQL 的服务名，并确保两个容器位于同一 Docker 网络。

启动：

```bash
docker compose -f docker-compose.prebuilt.yml pull
docker compose -f docker-compose.prebuilt.yml up -d
docker compose -f docker-compose.prebuilt.yml ps
docker compose -f docker-compose.prebuilt.yml logs -f recallhub
```

访问：

```text
http://服务器IP:8080/
http://服务器IP:8080/actuator/health
```

#### 更新 WAR

1. 按 Windows 或 Linux 构建步骤重新生成 WAR。
2. 确认 WAR 文件名仍为 `recall-hub-server-1.0.0.war`。
3. 强制重建 Tomcat 容器，使其重新解压 WAR：

Windows 或 Linux 均执行：

```bash
docker compose -f docker-compose.prebuilt.yml up -d --force-recreate
docker compose -f docker-compose.prebuilt.yml logs -f recallhub
```

停止：

```bash
docker compose -f docker-compose.prebuilt.yml down
```

该流程不会重新构建 Docker 镜像。第一次拉取 Tomcat 镜像后，后续更新只需要重新生成 WAR 和重建容器。

### 部署到外部 Tomcat

必须使用：

```text
Tomcat 10.1.x
JDK 17
```

不能使用 Tomcat 9。Spring Boot 3 使用 `jakarta.*`，Tomcat 9 仍是旧的 `javax.*` Servlet API。

#### 1. 构建包含前端的 WAR

按照本章开头的 Windows 或 Linux 手工构建步骤生成 WAR。不要只在 `server/` 中执行 Maven 打包，否则 WAR 中可能没有最新的 Vue 前端资源。

#### 2. Linux 原生 Tomcat

在 `$CATALINA_BASE/bin/setenv.sh` 中配置：

```bash
#!/usr/bin/env bash

export MYSQL_HOST="127.0.0.1"
export MYSQL_PORT="3306"
export MYSQL_DATABASE="recall_hub"
export MYSQL_USERNAME="recallhub"
export MYSQL_PASSWORD="替换成数据库密码"

export RECALLHUB_TIMEZONE="Asia/Shanghai"
export BACKUP_PATH="/var/lib/recallhub/backups"
export MYSQLDUMP_PATH="/usr/bin/mysqldump"
export BACKUP_ENABLED="true"

export CATALINA_OPTS="-Dfile.encoding=UTF-8 -Duser.timezone=UTC -Xms256m -Xmx1024m"
```

设置权限：

```bash
chmod 750 "$CATALINA_BASE/bin/setenv.sh"
```

停止 Tomcat，将原 ROOT 应用改名备份，然后部署 WAR：

```bash
sudo systemctl stop tomcat

if [ -d "$CATALINA_BASE/webapps/ROOT" ]; then
  sudo mv "$CATALINA_BASE/webapps/ROOT" \
    "$CATALINA_BASE/webapps/ROOT.backup.$(date +%Y%m%d%H%M%S)"
fi

sudo cp /path/to/recall-hub/server/target/recall-hub-server-1.0.0.war \
  "$CATALINA_BASE/webapps/ROOT.war"

sudo chown tomcat:tomcat "$CATALINA_BASE/webapps/ROOT.war"
sudo systemctl start tomcat
```

检查：

```bash
systemctl status tomcat
journalctl -u tomcat -f
curl -fsS http://127.0.0.1:8080/actuator/health
```

实际服务名和运行用户可能是 `tomcat10`，请根据服务器安装方式替换示例中的 `tomcat`。

#### 3. Windows 原生 Tomcat

如果使用 `startup.bat` 启动 Tomcat，在 `%CATALINA_HOME%\bin\setenv.bat` 中填写：

```bat
@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-17"

set "MYSQL_HOST=127.0.0.1"
set "MYSQL_PORT=3306"
set "MYSQL_DATABASE=recall_hub"
set "MYSQL_USERNAME=recallhub"
set "MYSQL_PASSWORD=替换成数据库密码"

set "RECALLHUB_TIMEZONE=Asia/Shanghai"
set "BACKUP_PATH=D:\RecallHub\backups"
set "MYSQLDUMP_PATH=I:\MySQL\mysql-5.7.23-winx64\bin\mysqldump.exe"
set "BACKUP_ENABLED=true"

set "JAVA_OPTS=-Dfile.encoding=UTF-8 -Duser.timezone=UTC -Xms256m -Xmx1024m"
```

OpenClaw Base URL 和 Hook Token 可以继续在 RecallHub 设置页配置。

如果 Tomcat 安装成了 Windows Service，`setenv.bat` 通常不会经过服务包装器加载。此时应在 Tomcat 服务配置器或 Windows 系统环境变量中设置同名变量，然后重启服务。

#### 4. Windows 以根应用部署

RecallHub 前端的 API、图标和路由均按根路径生成，因此 WAR 应部署为 `ROOT.war`。

停止 Tomcat：

```powershell
& "$env:CATALINA_HOME\bin\shutdown.bat"
```

首次部署前，备份 Tomcat 自带的 `webapps\ROOT` 目录。然后复制 WAR：

```powershell
Copy-Item `
  "I:\JAVA\IDEA代码\recall-hub\server\target\recall-hub-server-1.0.0.war" `
  "$env:CATALINA_HOME\webapps\ROOT.war" `
  -Force
```

如果存在上一次自动解压的 `webapps\ROOT` 目录，应在 Tomcat 停止状态下移走旧目录，让 Tomcat 从新 WAR 重新解压。

启动 Tomcat：

```powershell
& "$env:CATALINA_HOME\bin\startup.bat"
```

访问：

```text
http://Tomcat主机:8080/
http://Tomcat主机:8080/actuator/health
```

外部 Tomcat 的端口由 `%CATALINA_HOME%\conf\server.xml` 决定，`SERVER_PORT` 在 WAR 部署模式下不会控制容器端口。

#### 5. 更新版本

每次更新执行：

1. 重新运行 `deploy\build.ps1`。
2. 停止 Tomcat。
3. 替换 `webapps\ROOT.war`。
4. 移走上一次解压的 `webapps\ROOT`。
5. 启动 Tomcat。
6. 检查 `/actuator/health` 和 Tomcat 日志。

---

## 17. 常用检查命令

### 后端编译与测试

```powershell
cd "I:\JAVA\IDEA代码\recall-hub\server"
mvn test
```

### 前端类型检查

```powershell
cd "I:\JAVA\IDEA代码\recall-hub\web"
pnpm typecheck
```

### 前端生产构建

```powershell
cd "I:\JAVA\IDEA代码\recall-hub\web"
pnpm build
```

### 检查服务端口

```powershell
Get-NetTCPConnection -State Listen |
  Where-Object { $_.LocalPort -in 3306, 8080, 5173, 18789 } |
  Select-Object LocalAddress, LocalPort, OwningProcess
```

### 从 OpenClaw 主机检查 RecallHub

```bash
curl -sS http://192.168.3.79:8080/actuator/health
```

---

## 18. 常见问题

### Flyway 启动失败

检查：

1. MySQL 是否为 5.7.23。
2. `.env` 中的数据库名、用户名和密码是否正确。
3. 用户是否拥有 `recall_hub.*` 的全部权限。
4. 是否误用了已经存在但不受 Flyway 管理的同名表。

### 页面打开但 API 请求失败

确认后端 `8080` 正常：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
```

开发环境必须同时运行后端和 Vite。

### OpenClaw 显示在线，但提醒没有收到

“在线”只表示 RecallHub 能连接 Gateway 端口。继续检查：

1. Hook Token 是否正确。
2. 提醒渠道是否启用并设为默认。
3. `Channel`、`Target`、`Account ID` 是否和 OpenClaw 实际配置一致。
4. OpenClaw `/hooks/agent` 是否允许远程 RecallHub 主机访问。
5. Reminder 的 `last_error` 和 Delivery 日志。

### Skill 返回 `401`

原因通常是 `rh_...` API Token 错误、已撤销，或者误填了 Hook Token。重新在“设置 → 访问令牌”生成 Token，并更新：

```text
/home/menghanjun/.config/recallhub/config.json
```

### Skill 无法连接 RecallHub

在 `ai-agent` 执行：

```bash
curl -sS http://192.168.3.79:8080/actuator/health
```

如果失败，检查：

- RecallHub 是否监听 `8080`。
- Windows 防火墙是否允许来自 `ai-agent` 的连接。
- Skill URL 是否错误地写成了 `18789`。
- 两台机器是否能通过局域网或 Tailscale 相互访问。

### OpenClaw 找不到 Skill

检查：

```bash
test -f /home/menghanjun/.openclaw/workspace/skills/recall-hub/SKILL.md \
  && echo "SKILL.md exists"

openclaw skills info recall-hub
openclaw skills check
```

仍未出现时执行：

```bash
openclaw gateway restart
```

然后新开 OpenClaw 会话。

### 修改 OpenClaw URL 或 Token 后是否要重启

- RecallHub 设置页修改 Base URL 或 Hook Token：立即生效，不需要重启。
- 修改 `~/.config/recallhub/config.json`：Helper 下次运行立即生效。
- 新安装或更新 `SKILL.md`：通常由 watcher 自动发现；已有会话未刷新时，重启 Gateway 或新开会话。

---

## 19. 安全建议

- 不要提交 `.env`、Hook Token、`rh_...` Token、微信 Target 或 Account ID。
- `~/.config/recallhub/config.json` 必须设置为 `600`。
- 不要将 MySQL `3306` 和 OpenClaw `18789` 直接开放到公网。
- 远程访问 RecallHub 建议使用 Tailscale 或 HTTPS 反向代理。
- 定期检查备份是否能够恢复。
- API Token 遗失后立即撤销并重新生成。

---

## 20. 补充文档

- `项目文档.md`：完整 V1 设计与验收标准。
- `docs/architecture.md`：数据流和 Reminder Worker。
- `docs/api.md`：主要 REST API。
- `docs/database.md`：MySQL 5.7 与全文搜索。
- `docs/themes.md`：主题 Token 与导入限制。
- `docs/deployment.md`：生产部署。
