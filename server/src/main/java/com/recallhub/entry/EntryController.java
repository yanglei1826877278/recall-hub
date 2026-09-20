package com.recallhub.entry;

import com.recallhub.common.ApiResponse;
import com.recallhub.common.Types.EntryStatus;
import com.recallhub.common.Types.EntryType;
import com.recallhub.entry.EntryDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/entries")
@RequiredArgsConstructor
public class EntryController {
    private final EntryService service;

    @GetMapping
    @PreAuthorize("@authz.has('ENTRY_READ', authentication)")
    public ApiResponse<?> list(@RequestParam(required = false) EntryType type,
                               @RequestParam(required = false) EntryStatus status,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(type, status, from, to, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('ENTRY_READ', authentication)")
    public ApiResponse<?> get(@PathVariable long id) { return ApiResponse.ok(service.get(id)); }

    @PostMapping
    @PreAuthorize("@authz.has('ENTRY_WRITE', authentication)")
    public ApiResponse<?> create(@Valid @RequestBody CreateEntry body) { return ApiResponse.ok(service.create(body, null)); }

    @PatchMapping("/{id}")
    @PreAuthorize("@authz.has('ENTRY_WRITE', authentication)")
    public ApiResponse<?> update(@PathVariable long id, @Valid @RequestBody UpdateEntry body) {
        return ApiResponse.ok(service.update(id, body));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@authz.has('ENTRY_WRITE', authentication)")
    public ApiResponse<?> complete(@PathVariable long id) { return ApiResponse.ok(service.complete(id)); }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.has('ENTRY_WRITE', authentication)")
    public ApiResponse<Void> delete(@PathVariable long id) { service.delete(id); return ApiResponse.ok(); }
}

