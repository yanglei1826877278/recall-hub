package com.recallhub.reminder;

import com.recallhub.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class ReminderController {
    private final ReminderService service;

    @GetMapping @PreAuthorize("@authz.has('REMINDER_READ', authentication)")
    public ApiResponse<?> list(@RequestParam(required = false) String status,
                               @RequestParam(required = false) OffsetDateTime from,
                               @RequestParam(required = false) OffsetDateTime to) {
        return ApiResponse.ok(service.list(status, from, to));
    }
    @PostMapping @PreAuthorize("@authz.has('REMINDER_WRITE', authentication)")
    public ApiResponse<?> create(@Valid @RequestBody CreateReminder b) {
        return ApiResponse.ok(service.create(b.entryId(), b.remindAt(), b.notificationTargetId()));
    }
    @PatchMapping("/{id}") @PreAuthorize("@authz.has('REMINDER_WRITE', authentication)")
    public ApiResponse<?> update(@PathVariable long id, @RequestBody UpdateReminder b) {
        return ApiResponse.ok(service.update(id, b.remindAt(), b.notificationTargetId()));
    }
    @PostMapping("/{id}/cancel") @PreAuthorize("@authz.has('REMINDER_WRITE', authentication)")
    public ApiResponse<?> cancel(@PathVariable long id) { return ApiResponse.ok(service.cancel(id)); }
    @PostMapping("/{id}/snooze") @PreAuthorize("@authz.has('REMINDER_WRITE', authentication)")
    public ApiResponse<?> snooze(@PathVariable long id, @Valid @RequestBody Snooze b) {
        return ApiResponse.ok(service.snooze(id, b.until()));
    }
    public record CreateReminder(@NotNull Long entryId, @NotNull OffsetDateTime remindAt, Long notificationTargetId) {}
    public record UpdateReminder(OffsetDateTime remindAt, Long notificationTargetId) {}
    public record Snooze(@NotNull OffsetDateTime until) {}
}

