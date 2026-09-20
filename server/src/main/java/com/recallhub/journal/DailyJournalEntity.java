package com.recallhub.journal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_journals")
public class DailyJournalEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate journalDate;
    private String generatedContent;
    private String content;
    private Boolean userEdited;
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

