package com.recallhub.share;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("journal_shares")
public class JournalShareEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long journalId;
    private String shareToken;
    private String passwordHash;
    private Integer passwordDigits;
    private Integer passwordVersion;
    private String contentSnapshot;
    private LocalDateTime snapshotUpdatedAt;
    private LocalDateTime sharedAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
