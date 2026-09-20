package com.recallhub.openclaw;

import com.fasterxml.jackson.databind.JsonNode;
import com.recallhub.config.RecallHubProperties;
import com.recallhub.notification.NotificationTargetEntity;
import com.recallhub.setting.AppSettingEntity;
import com.recallhub.setting.AppSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.LinkedHashMap;
import java.util.Map;
import java.net.URI;
import java.net.Socket;
import java.net.http.HttpClient;

@Service
@RequiredArgsConstructor
public class OpenClawClient {
    private final RecallHubProperties properties;
    private final AppSettingMapper settings;

    public HookResult deliver(NotificationTargetEntity target, String message, String idempotencyKey) {
        String hookToken = hookToken();
        if (hookToken == null || hookToken.isBlank())
            return HookResult.failure(null, null, "HOOK_NOT_CONFIGURED", "OPENCLAW_HOOK_TOKEN 未配置");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "请只发送下面这条提醒，不添加解释：\n" + message);
        payload.put("name", "RecallHub Reminder");
        payload.put("agentId", target.getAgentId() == null ? "main" : target.getAgentId());
        payload.put("deliver", true);
        payload.put("channel", target.getChannel());
        payload.put("to", target.getTarget());
        if (target.getAccountId() != null && !target.getAccountId().isBlank()) payload.put("accountId", target.getAccountId());
        payload.put("waitForCompletion", true);
        try {
            var response = client().post().uri("/hooks/agent")
                    .header("Authorization", "Bearer " + hookToken)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve().toEntity(JsonNode.class);
            JsonNode body = response.getBody();
            boolean ok = body != null && body.path("ok").asBoolean(false);
            JsonNode completion = body == null ? null : body.path("completion");
            if (completion != null && !completion.isMissingNode() && completion.has("status"))
                ok = ok && "ok".equalsIgnoreCase(completion.path("status").asText());
            if (body != null && body.has("delivered")) ok = ok && body.path("delivered").asBoolean(false);
            String runId = body == null ? null : body.path("runId").asText(null);
            return ok ? HookResult.success(response.getStatusCode().value(), runId,
                    body != null && body.has("deliveryAttempted") ? body.path("deliveryAttempted").asBoolean() : true,
                    body != null && body.has("delivered") ? body.path("delivered").asBoolean() : true)
                    : HookResult.failure(response.getStatusCode().value(), runId, "OPENCLAW_REJECTED",
                    body == null ? "OpenClaw 返回空响应" : body.toString());
        } catch (Exception ex) {
            return HookResult.failure(null, null, "OPENCLAW_REQUEST_FAILED", ex.getMessage());
        }
    }

    public boolean health() {
        try {
            URI uri = URI.create(baseUrl());
            int port = uri.getPort() > 0 ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80);
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(uri.getHost(), port),
                        (int) properties.openclaw().connectTimeout().toMillis());
                return true;
            }
        } catch (Exception ignored) { return false; }
    }

    public String baseUrl() {
        AppSettingEntity setting = settings.selectById("openclaw_base_url");
        String configured = setting == null ? null : setting.getSettingValue();
        String value = configured == null || configured.isBlank() ? properties.openclaw().baseUrl() : configured.trim();
        URI uri;
        try { uri = URI.create(value); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("OpenClaw Base URL 格式无效"); }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null)
            throw new IllegalStateException("OpenClaw Base URL 必须是有效的 HTTP 或 HTTPS 地址");
        return value.replaceAll("/+$", "");
    }

    public String hookToken() {
        AppSettingEntity setting = settings.selectById("openclaw_hook_token");
        String configured = setting == null ? null : setting.getSettingValue();
        return configured == null || configured.isBlank() ? properties.openclaw().hookToken() : configured.trim();
    }

    public boolean hookTokenConfigured() {
        String token = hookToken();
        return token != null && !token.isBlank();
    }

    private RestClient client() {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.openclaw().connectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.openclaw().requestTimeout());
        return RestClient.builder().baseUrl(baseUrl()).requestFactory(factory).build();
    }

    public record HookResult(boolean success, Integer httpStatus, String runId, boolean deliveryAttempted,
                             boolean delivered, String errorCode, String errorMessage) {
        static HookResult success(Integer status, String runId, boolean attempted, boolean delivered) {
            return new HookResult(true, status, runId, attempted, delivered, null, null);
        }
        static HookResult failure(Integer status, String runId, String code, String message) {
            return new HookResult(false, status, runId, false, false, code,
                    message == null ? code : message.substring(0, Math.min(message.length(), 1000)));
        }
    }
}
