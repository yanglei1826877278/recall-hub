package com.recallhub.backup;

import com.recallhub.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/backups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class BackupController {
    private final BackupService service;
    @PostMapping public ApiResponse<?> backup(){return ApiResponse.ok(service.create());}
}

