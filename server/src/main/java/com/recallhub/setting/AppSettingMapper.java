package com.recallhub.setting;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AppSettingMapper extends BaseMapper<AppSettingEntity> {
    @Insert("INSERT INTO app_settings(setting_key, setting_value) VALUES(#{key},#{value}) " +
            "ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)")
    int upsert(@Param("key") String key, @Param("value") String value);
}

