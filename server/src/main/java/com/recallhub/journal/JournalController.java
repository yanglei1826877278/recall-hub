package com.recallhub.journal;

import com.recallhub.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/journals")
@RequiredArgsConstructor
public class JournalController {
    private final JournalService service;
    @GetMapping("/{date}") @PreAuthorize("@authz.has('JOURNAL_READ', authentication)")
    public ApiResponse<?> get(@PathVariable LocalDate date) { return ApiResponse.ok(service.get(date)); }
    @GetMapping("/{date}/entries") @PreAuthorize("@authz.has('JOURNAL_READ', authentication)")
    public ApiResponse<?> entries(@PathVariable LocalDate date) { return ApiResponse.ok(service.entries(date)); }
    @PutMapping("/{date}") @PreAuthorize("@authz.has('JOURNAL_WRITE', authentication)")
    public ApiResponse<?> put(@PathVariable LocalDate date, @RequestBody JournalBody body) {
        return ApiResponse.ok(service.put(date, body.content()));
    }
    public record JournalBody(String content) {}
}

