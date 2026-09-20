package com.recallhub.search;

import com.recallhub.common.ApiResponse;
import com.recallhub.common.TimeMapper;
import com.recallhub.common.Types.EntryStatus;
import com.recallhub.common.Types.EntryType;
import com.recallhub.entry.EntryMapper;
import com.recallhub.entry.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    private final EntryMapper mapper;
    private final EntryService entries;
    private final TimeMapper time;

    @GetMapping @PreAuthorize("@authz.has('SEARCH', authentication)")
    public ApiResponse<?> search(@RequestParam String q, @RequestParam(required = false) EntryType type,
                                 @RequestParam(required = false) EntryStatus status,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100); long offset = (long) (Math.max(page, 1) - 1) * safeSize;
        String query = q.trim();
        if (query.isEmpty()) return ApiResponse.ok(Map.of("items", java.util.List.of(), "total", 0));
        var items = mapper.fullTextSearch(query, type == null ? null : type.name(), status == null ? null : status.name(),
                from == null ? null : time.startOfDayUtc(from), to == null ? null : time.endExclusiveUtc(to), offset, safeSize)
                .stream().map(entries::view).toList();
        long total = mapper.fullTextCount(query, type == null ? null : type.name(), status == null ? null : status.name(),
                from == null ? null : time.startOfDayUtc(from), to == null ? null : time.endExclusiveUtc(to));
        return ApiResponse.ok(Map.of("items", items, "total", total, "page", Math.max(page, 1), "size", safeSize));
    }
}

