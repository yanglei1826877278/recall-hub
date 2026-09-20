package com.recallhub.reminder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recallhub.common.BusinessException;
import com.recallhub.common.TimeMapper;
import com.recallhub.entry.EntryEntity;
import com.recallhub.entry.EntryMapper;
import com.recallhub.notification.NotificationTargetEntity;
import com.recallhub.notification.NotificationTargetMapper;
import com.recallhub.openclaw.OpenClawClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final ReminderMapper mapper;
    private final ReminderDeliveryMapper deliveries;
    private final NotificationTargetMapper targets;
    private final EntryMapper entries;
    private final OpenClawClient openClaw;
    private final TimeMapper time;
    private final ZoneId appZoneId;

    @Transactional
    public ReminderView create(long entryId, OffsetDateTime remindAt, Long targetId) {
        if (entries.selectById(entryId) == null) throw new BusinessException("ENTRY_NOT_FOUND", "记录不存在", HttpStatus.NOT_FOUND);
        if (remindAt == null) throw new BusinessException("REMIND_AT_REQUIRED", "提醒时间不能为空");
        ReminderEntity entity = new ReminderEntity();
        entity.setEntryId(entryId); entity.setNotificationTargetId(targetId);
        entity.setRemindAt(time.toUtc(remindAt)); entity.setStatus("SCHEDULED"); entity.setAttemptCount(0);
        mapper.insert(entity); return view(mapper.selectById(entity.getId()));
    }

    public List<ReminderView> list(String status, OffsetDateTime from, OffsetDateTime to) {
        return mapper.selectList(new LambdaQueryWrapper<ReminderEntity>()
                .eq(status != null, ReminderEntity::getStatus, status)
                .ge(from != null, ReminderEntity::getRemindAt, time.toUtc(from))
                .lt(to != null, ReminderEntity::getRemindAt, time.toUtc(to))
                .orderByAsc(ReminderEntity::getRemindAt)).stream().map(this::view).toList();
    }

    @Transactional public ReminderView update(long id, OffsetDateTime at, Long targetId) {
        ReminderEntity e = require(id);
        if (at != null) e.setRemindAt(time.toUtc(at));
        if (targetId != null) e.setNotificationTargetId(targetId);
        e.setStatus("SCHEDULED"); e.setNextAttemptAt(null); e.setLeaseUntil(null); e.setLastError(null);
        mapper.updateById(e); return view(mapper.selectById(id));
    }

    @Transactional public ReminderView cancel(long id) {
        ReminderEntity e = require(id); e.setStatus("CANCELLED");
        e.setCancelledAt(LocalDateTime.now(Clock.systemUTC())); e.setLeaseUntil(null);
        e.setNextAttemptAt(null); mapper.updateById(e); return view(e);
    }

    @Transactional public ReminderView snooze(long id, OffsetDateTime until) {
        if (until == null || !until.isAfter(OffsetDateTime.now()))
            throw new BusinessException("INVALID_SNOOZE", "延后时间必须晚于当前时间");
        ReminderEntity e = require(id); e.setRemindAt(time.toUtc(until)); e.setStatus("SCHEDULED");
        e.setAttemptCount(0); e.setNextAttemptAt(null); e.setLeaseUntil(null); e.setLastError(null);
        mapper.updateById(e); return view(e);
    }

    @Transactional public boolean claim(long id, LocalDateTime now) {
        return mapper.claim(id, now, now.plusMinutes(2)) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(long id) {
        ReminderEntity reminder = require(id);
        if (!"PROCESSING".equals(reminder.getStatus())) return;
        EntryEntity entry = entries.selectById(reminder.getEntryId());
        NotificationTargetEntity target = resolveTarget(reminder.getNotificationTargetId());
        int attempt = reminder.getAttemptCount();
        ReminderDeliveryEntity delivery = new ReminderDeliveryEntity();
        delivery.setReminderId(id); delivery.setAttemptNo(attempt);
        delivery.setIdempotencyKey("recallhub:reminder:" + id + ":delivery:" + attempt);
        delivery.setStatus("PROCESSING"); delivery.setStartedAt(LocalDateTime.now(Clock.systemUTC()));
        delivery.setDeliveryAttempted(false); delivery.setDelivered(false); deliveries.insert(delivery);

        if (entry == null) {
            LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
            delivery.setStatus("CANCELLED"); delivery.setFinishedAt(now);
            delivery.setErrorCode("ENTRY_NOT_FOUND");
            delivery.setErrorMessage("提醒关联的记录已删除，停止投递");
            deliveries.updateById(delivery);
            reminder.setStatus("CANCELLED"); reminder.setCancelledAt(now);
            reminder.setLeaseUntil(null); reminder.setNextAttemptAt(null);
            reminder.setLastError("提醒关联的记录已删除，已自动取消");
            mapper.updateById(reminder);
            return;
        }

        OpenClawClient.HookResult result;
        if (target == null) result = new OpenClawClient.HookResult(false, null, null, false, false,
                "TARGET_NOT_FOUND", "没有可用的通知渠道");
        else result = openClaw.deliver(target, formatMessage(entry, reminder), delivery.getIdempotencyKey());

        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        delivery.setFinishedAt(now); delivery.setHttpStatus(result.httpStatus()); delivery.setOpenclawRunId(result.runId());
        delivery.setDeliveryAttempted(result.deliveryAttempted()); delivery.setDelivered(result.delivered());
        delivery.setErrorCode(result.errorCode()); delivery.setErrorMessage(result.errorMessage());
        delivery.setStatus(result.success() ? "SENT" : "FAILED"); deliveries.updateById(delivery);
        if (result.success()) {
            reminder.setStatus("SENT"); reminder.setSentAt(now); reminder.setLeaseUntil(null); reminder.setLastError(null);
        } else if (attempt >= 4) {
            reminder.setStatus("FAILED"); reminder.setLeaseUntil(null); reminder.setLastError(result.errorMessage());
        } else {
            int delay = attempt == 1 ? 1 : attempt == 2 ? 5 : 15;
            reminder.setStatus("SCHEDULED"); reminder.setNextAttemptAt(now.plusMinutes(delay));
            reminder.setLeaseUntil(null); reminder.setLastError(result.errorMessage());
        }
        mapper.updateById(reminder);
    }

    private NotificationTargetEntity resolveTarget(Long id) {
        if (id != null) {
            NotificationTargetEntity target = targets.selectById(id);
            return target != null && Boolean.TRUE.equals(target.getEnabled()) ? target : null;
        }
        return targets.selectOne(new LambdaQueryWrapper<NotificationTargetEntity>()
                .eq(NotificationTargetEntity::getEnabled, true).eq(NotificationTargetEntity::getIsDefault, true).last("LIMIT 1"));
    }

    private String formatMessage(EntryEntity entry, ReminderEntity reminder) {
        String text = entry.getTitle() != null && !entry.getTitle().isBlank() ? entry.getTitle() : entry.getContent();
        ZonedDateTime planned = reminder.getRemindAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(appZoneId);
        boolean delayed = Duration.between(planned.toInstant(), Instant.now()).toMinutes() >= 5;
        return (delayed ? "🔔 延迟提醒\n\n" : "🔔 提醒\n\n") + text +
                (delayed ? "\n\n原计划：" + planned.format(DateTimeFormatter.ofPattern("M月d日 HH:mm")) : "");
    }

    private ReminderEntity require(long id) {
        ReminderEntity e = mapper.selectById(id);
        if (e == null) throw new BusinessException("REMINDER_NOT_FOUND", "提醒不存在", HttpStatus.NOT_FOUND);
        return e;
    }

    public ReminderView view(ReminderEntity e) {
        return new ReminderView(e.getId(), e.getEntryId(), e.getNotificationTargetId(), time.fromUtc(e.getRemindAt()),
                e.getStatus(), e.getAttemptCount(), time.fromUtc(e.getNextAttemptAt()), e.getLastError(),
                time.fromUtc(e.getSentAt()), time.fromUtc(e.getCreatedAt()));
    }

    public record ReminderView(Long id, Long entryId, Long notificationTargetId, OffsetDateTime remindAt,
                               String status, Integer attemptCount, OffsetDateTime nextAttemptAt, String lastError,
                               OffsetDateTime sentAt, OffsetDateTime createdAt) {}
}
