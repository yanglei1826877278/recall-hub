package com.recallhub.journal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recallhub.common.TimeMapper;
import com.recallhub.entry.EntryEntity;
import com.recallhub.entry.EntryMapper;
import com.recallhub.entry.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JournalService {
    private final DailyJournalMapper journals;
    private final EntryMapper entries;
    private final EntryService entryService;
    private final TimeMapper time;

    public Object get(LocalDate date) {
        DailyJournalEntity journal = journals.selectOne(new LambdaQueryWrapper<DailyJournalEntity>()
                .eq(DailyJournalEntity::getJournalDate, date));
        return Map.of("date", date, "journal", journal == null ? Map.of() : journal,
                "entries", entries(date));
    }

    public Object entries(LocalDate date) {
        return entries.byEffectiveRange("DIARY", time.startOfDayUtc(date), time.endExclusiveUtc(date))
                .stream().map(entryService::view).toList();
    }

    @Transactional
    public DailyJournalEntity put(LocalDate date, String content) {
        DailyJournalEntity journal = journals.selectOne(new LambdaQueryWrapper<DailyJournalEntity>()
                .eq(DailyJournalEntity::getJournalDate, date));
        if (journal == null) {
            journal = new DailyJournalEntity(); journal.setJournalDate(date);
            journal.setContent(content); journal.setUserEdited(true); journals.insert(journal);
        } else {
            journal.setContent(content); journal.setUserEdited(true); journals.updateById(journal);
        }
        return journals.selectById(journal.getId());
    }
}
