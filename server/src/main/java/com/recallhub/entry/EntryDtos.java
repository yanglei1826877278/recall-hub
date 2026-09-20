package com.recallhub.entry;

import com.recallhub.common.Types.EntryStatus;
import com.recallhub.common.Types.EntryType;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public final class EntryDtos {
    private EntryDtos() {}

    public record CreateEntry(EntryType type, @Size(max = 255) String title, String content,
                              OffsetDateTime occurredAt, OffsetDateTime dueAt) {}
    public record UpdateEntry(EntryType type, @Size(max = 255) String title, String content,
                              EntryStatus status, OffsetDateTime occurredAt, OffsetDateTime dueAt) {}
    public record EntryView(Long id, EntryType type, String title, String content, EntryStatus status,
                            OffsetDateTime occurredAt, OffsetDateTime dueAt, OffsetDateTime completedAt,
                            Long sourceId, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                            String reminderStatus, OffsetDateTime remindAt, OffsetDateTime reminderSentAt) {}
}
