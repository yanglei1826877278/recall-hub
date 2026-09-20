package com.recallhub.capture;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_idempotency")
public class IdempotencyEntity {
    @TableId
    private String idempotencyKey;
    private String requestPath;
    private String requestHash;
    private String resourceType;
    private Long resourceId;
    private String responseBody;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}

