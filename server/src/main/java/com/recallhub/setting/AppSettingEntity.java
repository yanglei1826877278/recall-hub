package com.recallhub.setting;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("app_settings")
public class AppSettingEntity {
    @TableId
    private String settingKey;
    private String settingValue;
    private LocalDateTime updatedAt;
}

