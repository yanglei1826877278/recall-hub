package com.recallhub.journal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recallhub.ai.AiResponsesClient;
import com.recallhub.common.BusinessException;
import com.recallhub.common.TimeMapper;
import com.recallhub.entry.EntryEntity;
import com.recallhub.entry.EntryMapper;
import com.recallhub.entry.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JournalService {
    private final DailyJournalMapper journals;
    private final EntryMapper entries;
    private final EntryService entryService;
    private final TimeMapper time;
    private final AiResponsesClient ai;

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

    public DailyJournalEntity generate(LocalDate date) {
        List<EntryEntity> sourceEntries = sourceEntries(date);
        if (sourceEntries.isEmpty()) {
            throw new BusinessException("JOURNAL_SOURCE_EMPTY", "这一天还没有原始片段，先记下一些内容吧");
        }

        String generated = ai.generateJournal(buildPrompt(date, sourceEntries));
        DailyJournalEntity journal = journals.selectOne(new LambdaQueryWrapper<DailyJournalEntity>()
                .eq(DailyJournalEntity::getJournalDate, date));
        if (journal == null) {
            journal = new DailyJournalEntity();
            journal.setJournalDate(date);
            journal.setGeneratedContent(generated);
            journal.setContent(generated);
            journal.setUserEdited(false);
            journal.setGeneratedAt(LocalDateTime.now(ZoneOffset.UTC));
            journals.insert(journal);
        } else {
            journal.setGeneratedContent(generated);
            journal.setContent(generated);
            journal.setUserEdited(false);
            journal.setGeneratedAt(LocalDateTime.now(ZoneOffset.UTC));
            journals.updateById(journal);
        }
        return journals.selectById(journal.getId());
    }

    private List<EntryEntity> sourceEntries(LocalDate date) {
        return entries.byEffectiveRange("DIARY", time.startOfDayUtc(date), time.endExclusiveUtc(date));
    }

    private String buildPrompt(LocalDate date, List<EntryEntity> sourceEntries) {
        final int maxSourceLength = 120_000;
        StringBuilder prompt = new StringBuilder("请把以下 ")
                .append(date).append(" 的原始片段整理成一篇连贯、自然的第一人称中文日记。\n")
                .append("要求：按时间顺序组织；合并重复信息；保留具体事实和细节；可修正明显语病；不要添加素材中没有的信息。\n\n原始片段：\n");
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
        for (EntryEntity entry : sourceEntries) {
            var occurredAt = time.fromUtc(entry.getOccurredAt() == null ? entry.getCreatedAt() : entry.getOccurredAt());
            prompt.append('[').append(occurredAt == null ? "未知时间" : occurredAt.format(timeFormat)).append("] ");
            if (entry.getTitle() != null && !entry.getTitle().isBlank()) {
                prompt.append(entry.getTitle().trim()).append("：");
            }
            if (entry.getContent() != null) {
                String content = entry.getContent().trim();
                int remaining = maxSourceLength - prompt.length();
                if (remaining <= 0) break;
                prompt.append(content, 0, Math.min(content.length(), remaining));
            }
            prompt.append('\n');
        }
        return prompt.toString();
    }
}
