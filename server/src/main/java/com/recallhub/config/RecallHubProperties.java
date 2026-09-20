package com.recallhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "recallhub")
public record RecallHubProperties(
        String timezone,
        OpenClaw openclaw,
        Backup backup
) {
    public record OpenClaw(String baseUrl, String hookToken, Duration connectTimeout, Duration requestTimeout) {}
    public record Backup(String path, String mysqldump, boolean enabled) {}
}

