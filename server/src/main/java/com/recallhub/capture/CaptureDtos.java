package com.recallhub.capture;

import com.fasterxml.jackson.databind.JsonNode;
import com.recallhub.common.Types.Channel;
import com.recallhub.common.Types.SourceType;
import com.recallhub.entry.EntryDtos.CreateEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public final class CaptureDtos {
    private CaptureDtos() {}
    public record SourceInput(SourceType sourceType, Channel channel, String conversationId,
                              String messageId, String rawContent, JsonNode metadata) {}
    public record ReminderInput(@NotNull OffsetDateTime remindAt, Long notificationTargetId) {}
    public record CaptureRequest(@NotNull @Valid CreateEntry entry, @Valid ReminderInput reminder,
                                 @Valid SourceInput source) {}
    public record BatchCaptureRequest(@NotEmpty List<@Valid CreateEntry> entries, @Valid SourceInput source) {}
    public record CaptureResult(Object entry, Object reminder, Long sourceId) {}
    public record BatchCaptureResult(List<?> entries, Long sourceId) {}
}

