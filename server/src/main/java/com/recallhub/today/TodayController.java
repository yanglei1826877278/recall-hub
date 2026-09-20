package com.recallhub.today;

import com.recallhub.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/today")
@RequiredArgsConstructor
public class TodayController {
    private final TodayService service;
    @GetMapping @PreAuthorize("@authz.has('ENTRY_READ', authentication)")
    public ApiResponse<?> today() { return ApiResponse.ok(service.get()); }
}

