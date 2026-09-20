package com.recallhub.capture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recallhub.capture.CaptureDtos.*;
import com.recallhub.common.BusinessException;
import com.recallhub.entry.EntryService;
import com.recallhub.reminder.ReminderService;
import com.recallhub.source.SourceEntity;
import com.recallhub.source.SourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class CaptureService {
    private final SourceMapper sources;
    private final EntryService entries;
    private final ReminderService reminders;
    private final IdempotencyMapper idempotency;
    private final ObjectMapper objectMapper;

    @Transactional
    public Object capture(CaptureRequest body, String key) {
        String hash = hash(body);
        Object replay = replay(key, hash);
        if (replay != null) return replay;
        Long sourceId = createSource(body.source());
        var entry = entries.create(body.entry(), sourceId);
        Object reminder = body.reminder() == null ? null : reminders.create(entry.id(), body.reminder().remindAt(),
                body.reminder().notificationTargetId());
        var result = new CaptureResult(entry, reminder, sourceId);
        remember(key, "/api/v1/captures", hash, "ENTRY", entry.id(), result);
        return result;
    }

    @Transactional
    public Object batch(BatchCaptureRequest body, String key) {
        String hash = hash(body);
        Object replay = replay(key, hash);
        if (replay != null) return replay;
        Long sourceId = createSource(body.source());
        var resultEntries = new ArrayList<>();
        body.entries().forEach(e -> resultEntries.add(entries.create(e, sourceId)));
        var result = new BatchCaptureResult(resultEntries, sourceId);
        remember(key, "/api/v1/captures/batch", hash, "BATCH", null, result);
        return result;
    }

    private Long createSource(SourceInput input) {
        if (input == null) return null;
        SourceEntity source = new SourceEntity();
        source.setSourceType((input.sourceType() == null ? com.recallhub.common.Types.SourceType.OPENCLAW : input.sourceType()).name());
        source.setChannel((input.channel() == null ? com.recallhub.common.Types.Channel.UNKNOWN : input.channel()).name());
        source.setConversationId(input.conversationId()); source.setMessageId(input.messageId());
        source.setRawContent(input.rawContent());
        if (input.metadata() != null) source.setMetadata(input.metadata().toString());
        sources.insert(source); return source.getId();
    }

    private Object replay(String key, String hash) {
        if (key == null || key.isBlank()) return null;
        IdempotencyEntity old = idempotency.selectById(key);
        if (old == null || old.getExpiresAt().isBefore(LocalDateTime.now(Clock.systemUTC()))) return null;
        if (!old.getRequestHash().equals(hash))
            throw new BusinessException("IDEMPOTENCY_CONFLICT", "相同 Idempotency-Key 对应了不同请求", HttpStatus.CONFLICT);
        try { return objectMapper.readTree(old.getResponseBody()); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private void remember(String key, String path, String hash, String type, Long id, Object response) {
        if (key == null || key.isBlank()) return;
        if (key.length() > 255) throw new BusinessException("IDEMPOTENCY_KEY_TOO_LONG", "Idempotency-Key 不能超过 255 字符");
        try {
            IdempotencyEntity entity = new IdempotencyEntity();
            entity.setIdempotencyKey(key); entity.setRequestPath(path); entity.setRequestHash(hash);
            entity.setResourceType(type); entity.setResourceId(id); entity.setResponseBody(objectMapper.writeValueAsString(response));
            entity.setExpiresAt(LocalDateTime.now(Clock.systemUTC()).plusDays(7)); idempotency.insert(entity);
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private String hash(Object body) {
        try {
            byte[] encoded = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}

