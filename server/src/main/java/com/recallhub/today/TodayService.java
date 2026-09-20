package com.recallhub.today;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recallhub.common.TimeMapper;
import com.recallhub.entry.*;
import com.recallhub.journal.DailyJournalEntity;
import com.recallhub.journal.DailyJournalMapper;
import com.recallhub.reminder.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TodayService {
    private final EntryMapper entries;
    private final EntryService entryService;
    private final ReminderMapper reminders;
    private final ReminderService reminderService;
    private final DailyJournalMapper journals;
    private final TimeMapper time;
    private final Clock clock;

    public Object get() {
        LocalDate date = LocalDate.now(clock);
        LocalDateTime start = time.startOfDayUtc(date), end = time.endExclusiveUtc(date);
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        var overdue = entries.selectList(new LambdaQueryWrapper<EntryEntity>()
                .eq(EntryEntity::getType, "TODO").eq(EntryEntity::getStatus, "ACTIVE")
                .lt(EntryEntity::getDueAt, now).orderByAsc(EntryEntity::getDueAt))
                .stream().map(entryService::view).toList();
        var todos = entries.selectList(new LambdaQueryWrapper<EntryEntity>()
                .eq(EntryEntity::getType, "TODO").eq(EntryEntity::getStatus, "ACTIVE")
                .and(q -> q.isNull(EntryEntity::getDueAt).or().ge(EntryEntity::getDueAt, now))
                .orderByAsc(EntryEntity::getDueAt).orderByDesc(EntryEntity::getCreatedAt))
                .stream().map(entryService::view).toList();
        var todayEntries = entries.byEffectiveRange("DIARY", start, end);
        for (String type : new String[]{"NOTE", "IDEA"}) todayEntries.addAll(entries.byEffectiveRange(type, start, end));
        todayEntries.sort(java.util.Comparator.comparing(EntryEntity::getCreatedAt).reversed());
        var reminderViews = reminders.selectList(new LambdaQueryWrapper<ReminderEntity>()
                .in(ReminderEntity::getStatus, "SCHEDULED", "PROCESSING")
                .ge(ReminderEntity::getRemindAt, start).lt(ReminderEntity::getRemindAt, end)
                .orderByAsc(ReminderEntity::getRemindAt)).stream().map(reminderService::view).toList();
        DailyJournalEntity journal = journals.selectOne(new LambdaQueryWrapper<DailyJournalEntity>()
                .eq(DailyJournalEntity::getJournalDate, date));
        return Map.of("date", date, "overdue", overdue, "reminders", reminderViews, "todos", todos,
                "entries", todayEntries.stream().map(entryService::view).toList(),
                "journal", journal == null ? Map.of() : journal);
    }
}

