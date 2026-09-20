package com.recallhub.source;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sources")
public class SourceEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceType;
    private String channel;
    private String conversationId;
    private String messageId;
    private String rawContent;
    private String metadata;
    private LocalDateTime createdAt;
}

