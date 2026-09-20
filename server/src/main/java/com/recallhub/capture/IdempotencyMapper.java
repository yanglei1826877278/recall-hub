package com.recallhub.capture;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdempotencyMapper extends BaseMapper<IdempotencyEntity> {}

