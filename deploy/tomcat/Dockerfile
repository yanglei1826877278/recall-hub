FROM node:22-bookworm-slim AS web-builder

WORKDIR /build/web
RUN corepack enable && corepack prepare pnpm@9.15.0 --activate

COPY web/package.json web/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY web/ ./
RUN pnpm build


FROM maven:3.9-eclipse-temurin-17 AS server-builder

WORKDIR /build
COPY server/pom.xml server/pom.xml
RUN mvn -f server/pom.xml dependency:go-offline -B

COPY server/ server/
COPY --from=web-builder /build/web/dist/ /build/server/src/main/resources/static/
RUN mvn -f server/pom.xml clean package -DskipTests -B


FROM tomcat:10.1.60-jdk17-temurin-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /usr/local/tomcat/webapps/* \
    && groupadd --system recallhub \
    && useradd --system --gid recallhub --home-dir /usr/local/tomcat --no-create-home --shell /usr/sbin/nologin recallhub \
    && chown -R recallhub:recallhub /usr/local/tomcat

COPY --from=server-builder --chown=recallhub:recallhub \
    /build/server/target/recall-hub-server-1.0.0.war \
    /usr/local/tomcat/webapps/ROOT.war

ENV TZ=Asia/Shanghai \
    CATALINA_OPTS="-Dfile.encoding=UTF-8 -Duser.timezone=UTC -XX:MaxRAMPercentage=75.0" \
    RECALLHUB_TIMEZONE=Asia/Shanghai \
    BACKUP_PATH=/opt/recallhub/backups \
    BACKUP_ENABLED=false

RUN mkdir -p /opt/recallhub/backups \
    && chown -R recallhub:recallhub /opt/recallhub

USER recallhub
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl --fail --silent http://127.0.0.1:8080/actuator/health > /dev/null || exit 1

CMD ["catalina.sh", "run"]
