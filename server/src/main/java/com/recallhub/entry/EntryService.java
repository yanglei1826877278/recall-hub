package com.recallhub.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recallhub.common.BusinessException;
import com.recallhub.common.TimeMapper;
import com.recallhub.common.Types.EntryStatus;
import com.recallhub.common.Types.EntryType;
import com.recallhub.entry.EntryDtos.*;
import com.recallhub.reminder.ReminderMapper;
import com.recallhub.reminder.ReminderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EntryService {
    private final EntryMapper mapper;
    private final ReminderMapper reminderMapper;
    private final TimeMapper time;

    @Transactional
    public EntryView create(CreateEntry body, Long sourceId) {
        EntryType type = body.type() == null ? EntryType.NOTE : body.type();
        if ((body.title() == null || body.title().isBlank()) && (body.content() == null || body.content().isBlank()))
            throw new BusinessException("EMPTY_ENTRY", "标题和内容不能同时为空");
        EntryEntity entity = new EntryEntity();
        entity.setType(type.name());
        entity.setTitle(clean(body.title()));
        entity.setContent(clean(body.content()));
        entity.setStatus(EntryStatus.ACTIVE.name());
        entity.setOccurredAt(time.toUtc(body.occurredAt()));
        entity.setDueAt(time.toUtc(body.dueAt()));
        entity.setSourceId(sourceId);
        mapper.insert(entity);
        return view(mapper.selectById(entity.getId()));
    }

    public EntryView get(long id) { return view(require(id)); }

    public PageResult list(EntryType type, EntryStatus status, LocalDate from, LocalDate to, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        var query = new LambdaQueryWrapper<EntryEntity>()
                .eq(type != null, EntryEntity::getType, type == null ? null : type.name())
                .eq(status != null, EntryEntity::getStatus, status == null ? null : status.name())
                .ge(from != null, EntryEntity::getCreatedAt, from == null ? null : time.startOfDayUtc(from))
                .lt(to != null, EntryEntity::getCreatedAt, to == null ? null : time.endExclusiveUtc(to))
                .orderByDesc(EntryEntity::getCreatedAt).orderByDesc(EntryEntity::getId);
        Page<EntryEntity> result = mapper.selectPage(Page.of(Math.max(page, 1), safeSize), query);
        return new PageResult(result.getRecords().stream().map(this::view).toList(), result.getTotal(),
                result.getCurrent(), result.getSize(), result.getPages());
    }

    @Transactional
    public EntryView update(long id, UpdateEntry body) {
        EntryEntity entity = require(id);
        if (body.type() != null) entity.setType(body.type().name());
        if (body.title() != null) entity.setTitle(clean(body.title()));
        if (body.content() != null) entity.setContent(clean(body.content()));
        if (body.status() != null) entity.setStatus(body.status().name());
        if (body.occurredAt() != null) entity.setOccurredAt(time.toUtc(body.occurredAt()));
        if (body.dueAt() != null) entity.setDueAt(time.toUtc(body.dueAt()));
        mapper.updateById(entity);
        return view(mapper.selectById(id));
    }

    @Transactional
    public EntryView complete(long id) {
        EntryEntity entity = require(id);
        entity.setStatus(EntryStatus.DONE.name());
        entity.setCompletedAt(LocalDateTime.now(java.time.Clock.systemUTC()));
        mapper.updateById(entity);
        reminderMapper.cancelScheduledByEntry(id, LocalDateTime.now(java.time.Clock.systemUTC()));
        return view(mapper.selectById(id));
    }

    @Transactional
    public void delete(long id) {
        require(id);
        reminderMapper.cancelScheduledByEntry(id, LocalDateTime.now(java.time.Clock.systemUTC()));
        mapper.deleteById(id);
    }

    public EntryView view(EntryEntity e) {
        ReminderEntity reminder = reminderMapper.findLatestByEntryId(e.getId());
        return new EntryView(e.getId(), EntryType.valueOf(e.getType()), e.getTitle(), e.getContent(),
                EntryStatus.valueOf(e.getStatus()), time.fromUtc(e.getOccurredAt()), time.fromUtc(e.getDueAt()),
                time.fromUtc(e.getCompletedAt()), e.getSourceId(), time.fromUtc(e.getCreatedAt()),
                time.fromUtc(e.getUpdatedAt()), reminder == null ? null : reminder.getStatus(),
                reminder == null ? null : time.fromUtc(reminder.getRemindAt()),
                reminder == null ? null : time.fromUtc(reminder.getSentAt()));
    }

    public EntryEntity require(long id) {
        EntryEntity entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("ENTRY_NOT_FOUND", "记录不存在", HttpStatus.NOT_FOUND);
        return entity;
    }

    private String clean(String value) { return value == null ? null : value.trim(); }
    public record PageResult(List<EntryView> items, long total, long page, long size, long pages) {}
}
