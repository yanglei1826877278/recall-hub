package com.recallhub.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recallhub.common.BusinessException;
import com.recallhub.setting.AppSettingEntity;
import com.recallhub.setting.AppSettingMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AiResponsesClient {
    private static final String BASE_URL_KEY = "ai_base_url";
    private static final String API_KEY_KEY = "ai_api_key";
    private static final String MODEL_KEY = "ai_model";

    private final AppSettingMapper settings;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiResponsesClient(AppSettingMapper settings, ObjectMapper objectMapper) {
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String generateJournal(String prompt) {
        String baseUrl = setting(BASE_URL_KEY);
        String apiKey = setting(API_KEY_KEY);
        String model = setting(MODEL_KEY);
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) {
            throw new BusinessException("AI_NOT_CONFIGURED", "请先在设置中配置 AI Base URL、API Key 和模型");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("instructions", "你是一位严谨的中文日记编辑。只整理用户提供的事实，不虚构人物、事件、情绪或因果。输出纯文本日记正文，不要标题、Markdown 或说明。");
        payload.put("input", prompt);

        try {
            HttpRequest request = HttpRequest.newBuilder(responsesUri(baseUrl))
                    .timeout(Duration.ofMinutes(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = errorMessage(response.body());
                throw new BusinessException("AI_REQUEST_FAILED",
                        "AI 服务请求失败（HTTP " + response.statusCode() + "）" + (message.isBlank() ? "" : "：" + message),
                        HttpStatus.BAD_GATEWAY);
            }
            String text = extractOutputText(objectMapper.readTree(response.body()));
            if (text.isBlank()) {
                throw new BusinessException("AI_EMPTY_RESPONSE", "AI 服务没有返回可用的日记内容", HttpStatus.BAD_GATEWAY);
            }
            return text.trim();
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("AI_REQUEST_INTERRUPTED", "AI 整理被中断，请稍后重试", HttpStatus.BAD_GATEWAY);
        } catch (Exception ex) {
            throw new BusinessException("AI_REQUEST_FAILED", "无法连接 AI 服务：" + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    private String setting(String key) {
        AppSettingEntity value = settings.selectById(key);
        return value == null || value.getSettingValue() == null ? "" : value.getSettingValue().trim();
    }

    private URI responsesUri(String baseUrl) {
        String normalized = baseUrl.trim().replaceAll("/+$", "");
        return URI.create(normalized.endsWith("/responses") ? normalized : normalized + "/responses");
    }

    private String errorMessage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode message = root.path("error").path("message");
            return message.isTextual() ? message.asText() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    static String extractOutputText(JsonNode root) {
        JsonNode shortcut = root.get("output_text");
        if (shortcut != null && shortcut.isTextual() && !shortcut.asText().isBlank()) {
            return shortcut.asText();
        }
        StringBuilder result = new StringBuilder();
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) continue;
                for (JsonNode part : content) {
                    if ("output_text".equals(part.path("type").asText()) && part.path("text").isTextual()) {
                        if (!result.isEmpty()) result.append('\n');
                        result.append(part.path("text").asText());
                    }
                }
            }
        }
        return result.toString();
    }
}
