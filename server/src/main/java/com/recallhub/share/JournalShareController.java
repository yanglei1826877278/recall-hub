package com.recallhub.share;

import com.recallhub.common.ApiResponse;
import com.recallhub.share.JournalShareDtos.PasswordBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/journals/{date}/share")
@RequiredArgsConstructor
public class JournalShareController {
    private final JournalShareService service;

    @GetMapping
    @PreAuthorize("@authz.has('JOURNAL_READ', authentication)")
    public ApiResponse<?> status(@PathVariable LocalDate date) {
        return ApiResponse.ok(service.status(date));
    }

    @PostMapping
    @PreAuthorize("@authz.has('JOURNAL_WRITE', authentication)")
    public ApiResponse<?> create(@PathVariable LocalDate date, @Valid @RequestBody PasswordBody body) {
        return ApiResponse.ok(service.create(date, body.password()));
    }

    @PatchMapping("/password")
    @PreAuthorize("@authz.has('JOURNAL_WRITE', authentication)")
    public ApiResponse<?> updatePassword(@PathVariable LocalDate date, @Valid @RequestBody PasswordBody body) {
        return ApiResponse.ok(service.updatePassword(date, body.password()));
    }

    @PutMapping("/content")
    @PreAuthorize("@authz.has('JOURNAL_WRITE', authentication)")
    public ApiResponse<?> updateContent(@PathVariable LocalDate date) {
        return ApiResponse.ok(service.updateContent(date));
    }

    @DeleteMapping
    @PreAuthorize("@authz.has('JOURNAL_WRITE', authentication)")
    public ApiResponse<Void> revoke(@PathVariable LocalDate date) {
        service.revoke(date);
        return ApiResponse.ok();
    }
}
