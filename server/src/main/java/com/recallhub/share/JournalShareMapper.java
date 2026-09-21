package com.recallhub.share;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JournalShareMapper extends BaseMapper<JournalShareEntity> {
    @Select("SELECT * FROM journal_shares WHERE journal_id=#{journalId} LIMIT 1")
    JournalShareEntity findByJournalId(long journalId);

    @Select("SELECT * FROM journal_shares WHERE share_token=#{token} LIMIT 1")
    JournalShareEntity findByToken(String token);
}
