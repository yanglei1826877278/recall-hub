# 部署

生产环境需要 Java 17、MySQL 5.7、Node.js 22、pnpm 和 `mysqldump`。执行构建脚本后，将 `server/target/recall-hub-server-1.0.0.war` 部署到 `/opt/recallhub/recall-hub-server.war`。

将环境变量写入 `/etc/recallhub/recallhub.env`，权限设为仅服务账户可读。复制 `deploy/recallhub.service` 到 systemd 后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now recallhub
curl http://127.0.0.1:8080/actuator/health
```

不要把 MySQL 3306 或 OpenClaw 18789 暴露到公网。手机访问建议使用 Tailscale 或带 HTTPS 的反向代理。

## 外部 Tomcat

项目生成的 WAR 也可部署到 Tomcat 10.1.x。复制为 `$CATALINA_BASE/webapps/ROOT.war`，使前端绝对路径和 API 根路径保持正确。Tomcat 9 使用旧 Servlet API，不兼容 Spring Boot 3。

外部 Tomcat 的数据库配置应通过容器进程环境变量提供。使用脚本启动时可写入 `bin/setenv.sh` 或 `bin/setenv.bat`；使用 Windows Service 时需要在服务配置或系统环境变量中设置。

## Docker Tomcat

执行以下命令使用官方 Tomcat 10.1 + JDK 17 镜像构建并运行：

```bash
cp .env.docker.example .env.docker
docker compose build --pull
docker compose up -d
docker compose ps
```

Docker Desktop 访问宿主机 MySQL 时设置 `MYSQL_HOST=host.docker.internal`。Linux Docker 由 Compose 的 `host-gateway` 映射提供相同主机名。运行镜像以 `ROOT.war` 部署，因此 Web、API 和前端资源都位于根路径。

`OPENCLAW_BASE_URL` 是首次启动的默认地址。登录后可在“设置 → OpenClaw”中保存新的 Base URL，数据库设置会立即覆盖环境变量，适用于 OpenClaw 运行在局域网、Tailscale 或远程 HTTPS 主机的情况。

`OPENCLAW_HOOK_TOKEN` 也可以在“设置 → OpenClaw”中配置。数据库中的 Token 优先于环境变量，保存后 API 和界面均不会回显明文；清除数据库 Token 后会重新使用环境变量。
