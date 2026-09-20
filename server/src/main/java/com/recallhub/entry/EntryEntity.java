package com.recallhub.entry;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("entries")
public class EntryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String title;
    private String content;
    private String status;
    private LocalDateTime occurredAt;
    private LocalDateTime dueAt;
    private LocalDateTime completedAt;
    private Long sourceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic(value = "null", delval = "NOW(3)")
    private LocalDateTime deletedAt;
}

