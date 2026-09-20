package com.recallhub.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_tokens")
public class ApiTokenEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String tokenPrefix;
    private String tokenHash;
    private String scopes;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}

