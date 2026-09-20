package com.recallhub.system;

import com.recallhub.common.ApiResponse;
import com.recallhub.config.RecallHubProperties;
import com.recallhub.openclaw.OpenClawClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class SystemController {
    private final OpenClawClient client;
    private final RecallHubProperties properties;
    @GetMapping("/openclaw/status")
    public ApiResponse<?> openClawStatus() {
        return ApiResponse.ok(Map.of("online", client.health(), "baseUrl", client.baseUrl(),
                "tokenConfigured", client.hookTokenConfigured()));
    }
}
