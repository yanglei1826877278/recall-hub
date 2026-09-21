package com.recallhub.setting;

import com.recallhub.common.ApiResponse;
import com.recallhub.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class SettingController {
    private static final String OPENCLAW_HOOK_TOKEN = "openclaw_hook_token";
    private static final String AI_API_KEY = "ai_api_key";
    private static final Set<String> EDITABLE = Set.of("timezone", "appearance", "raw_chat_retention",
            "default_notification_target", "openclaw_base_url", OPENCLAW_HOOK_TOKEN,
            "ai_base_url", AI_API_KEY, "ai_model");
    private final AppSettingMapper mapper;
    @GetMapping public ApiResponse<?> all() {
        Map<String, String> result = new LinkedHashMap<>();
        boolean aiKeyConfigured = false;
        for (AppSettingEntity setting : mapper.selectList(null)) {
            if (OPENCLAW_HOOK_TOKEN.equals(setting.getSettingKey())) continue;
            if (AI_API_KEY.equals(setting.getSettingKey())) {
                aiKeyConfigured = setting.getSettingValue() != null && !setting.getSettingValue().isBlank();
                continue;
            }
            result.put(setting.getSettingKey(), setting.getSettingValue());
        }
        result.put("ai_api_key_configured", Boolean.toString(aiKeyConfigured));
        return ApiResponse.ok(result);
    }
    @PutMapping public ApiResponse<?> update(@RequestBody Map<String, String> values) {
        if (!EDITABLE.containsAll(values.keySet())) throw new BusinessException("SETTING_NOT_EDITABLE", "包含不可修改的设置项");
        Map<String, String> cleaned = new HashMap<>(values);
        if (cleaned.containsKey("openclaw_base_url")) validateHttpUrl(cleaned.get("openclaw_base_url"), "OpenClaw 地址");
        if (cleaned.containsKey("ai_base_url")) validateHttpUrl(cleaned.get("ai_base_url"), "AI Base URL");
        if (cleaned.containsKey("ai_model")) {
            String model = cleaned.get("ai_model");
            if (model != null && model.length() > 255)
                throw new BusinessException("AI_MODEL_TOO_LONG", "模型名称长度不能超过 255 个字符");
            cleaned.put("ai_model", model == null ? "" : model.trim());
        }
        if (cleaned.containsKey(OPENCLAW_HOOK_TOKEN)) {
            String token = cleaned.get(OPENCLAW_HOOK_TOKEN);
            if (token != null && token.length() > 4096)
                throw new BusinessException("HOOK_TOKEN_TOO_LONG", "Hook Token 长度不能超过 4096 个字符");
            cleaned.put(OPENCLAW_HOOK_TOKEN, token == null ? "" : token.trim());
        }
        if (cleaned.containsKey(AI_API_KEY)) {
            String token = cleaned.get(AI_API_KEY);
            if (token != null && token.length() > 8192)
                throw new BusinessException("AI_API_KEY_TOO_LONG", "API Key 长度不能超过 8192 个字符");
            cleaned.put(AI_API_KEY, token == null ? "" : token.trim());
        }
        cleaned.forEach(mapper::upsert); return all();
    }

    private void validateHttpUrl(String value, String label) {
        if (value == null || value.isBlank()) return;
        if (value.length() > 2048) throw new BusinessException("INVALID_URL", label + "过长");
        try {
            java.net.URI uri = java.net.URI.create(value.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_URL", label + "必须是有效的 HTTP 或 HTTPS URL，且不能包含查询参数");
        }
    }
}
