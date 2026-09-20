package com.recallhub.reminder;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reminder_deliveries")
public class ReminderDeliveryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reminderId;
    private Integer attemptNo;
    private String idempotencyKey;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer httpStatus;
    private String openclawRunId;
    private Boolean deliveryAttempted;
    private Boolean delivered;
    private String errorCode;
    private String errorMessage;
}

