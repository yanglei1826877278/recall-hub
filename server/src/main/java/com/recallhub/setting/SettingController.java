package com.recallhub.setting;

import com.recallhub.common.ApiResponse;
import com.recallhub.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class SettingController {
    private static final String OPENCLAW_HOOK_TOKEN = "openclaw_hook_token";
    private static final Set<String> EDITABLE = Set.of("timezone", "appearance", "raw_chat_retention",
            "default_notification_target", "openclaw_base_url", OPENCLAW_HOOK_TOKEN);
    private final AppSettingMapper mapper;
    @GetMapping public ApiResponse<?> all() {
        return ApiResponse.ok(mapper.selectList(null).stream()
                .filter(setting -> !OPENCLAW_HOOK_TOKEN.equals(setting.getSettingKey()))
                .collect(Collectors.toMap(
                AppSettingEntity::getSettingKey, AppSettingEntity::getSettingValue)));
    }
    @PutMapping public ApiResponse<?> update(@RequestBody Map<String, String> values) {
        if (!EDITABLE.containsAll(values.keySet())) throw new BusinessException("SETTING_NOT_EDITABLE", "包含不可修改的设置项");
        if (values.containsKey("openclaw_base_url")) validateOpenClawUrl(values.get("openclaw_base_url"));
        if (values.containsKey(OPENCLAW_HOOK_TOKEN)) {
            String token = values.get(OPENCLAW_HOOK_TOKEN);
            if (token != null && token.length() > 4096)
                throw new BusinessException("HOOK_TOKEN_TOO_LONG", "Hook Token 长度不能超过 4096 个字符");
            values.put(OPENCLAW_HOOK_TOKEN, token == null ? "" : token.trim());
        }
        values.forEach(mapper::upsert); return all();
    }

    private void validateOpenClawUrl(String value) {
        if (value == null || value.isBlank()) return;
        try {
            java.net.URI uri = java.net.URI.create(value.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_OPENCLAW_URL", "OpenClaw 地址必须是有效的 HTTP 或 HTTPS URL");
        }
    }
}
