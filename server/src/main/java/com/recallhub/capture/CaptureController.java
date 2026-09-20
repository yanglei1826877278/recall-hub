package com.recallhub.capture;

import com.recallhub.capture.CaptureDtos.*;
import com.recallhub.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/captures")
@RequiredArgsConstructor
@PreAuthorize("@authz.has('ENTRY_WRITE', authentication)")
public class CaptureController {
    private final CaptureService service;
    @PostMapping public ApiResponse<?> capture(@Valid @RequestBody CaptureRequest body,
                                               @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return ApiResponse.ok(service.capture(body, key));
    }
    @PostMapping("/batch") public ApiResponse<?> batch(@Valid @RequestBody BatchCaptureRequest body,
                                                        @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return ApiResponse.ok(service.batch(body, key));
    }
}

