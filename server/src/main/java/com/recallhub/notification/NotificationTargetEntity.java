package com.recallhub.notification;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification_targets")
public class NotificationTargetEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String channel;
    private String target;
    private String accountId;
    private String agentId;
    private Boolean enabled;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

