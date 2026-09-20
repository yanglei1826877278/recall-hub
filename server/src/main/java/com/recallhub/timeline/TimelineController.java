package com.recallhub.timeline;

import com.recallhub.common.ApiResponse;
import com.recallhub.common.CursorPage;
import com.recallhub.common.TimeMapper;
import com.recallhub.common.Types.EntryType;
import com.recallhub.entry.EntryMapper;
import com.recallhub.entry.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/timeline")
@RequiredArgsConstructor
public class TimelineController {
    private final EntryMapper mapper;
    private final EntryService entries;
    private final TimeMapper time;

    @GetMapping @PreAuthorize("@authz.has('ENTRY_READ', authentication)")
    public ApiResponse<?> timeline(@RequestParam(required = false) OffsetDateTime before,
                                   @RequestParam(required = false) OffsetDateTime after,
                                   @RequestParam(required = false) EntryType type,
                                   @RequestParam(defaultValue = "30") int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), 100);
        var found = mapper.timeline(time.toUtc(before), time.toUtc(after), type == null ? null : type.name(), size + 1);
        boolean more = found.size() > size;
        if (more) found = found.subList(0, size);
        var views = found.stream().map(entries::view).toList();
        String cursor = views.isEmpty() ? null : views.get(views.size() - 1).createdAt().toString();
        return ApiResponse.ok(new CursorPage<>(views, cursor, more));
    }
}

