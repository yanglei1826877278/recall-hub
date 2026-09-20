package com.recallhub.theme;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("themes")
public class ThemeEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String slug;
    private String lightVariables;
    private String darkVariables;
    private String sourceCss;
    private Boolean isBuiltin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

