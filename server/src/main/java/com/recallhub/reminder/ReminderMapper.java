package com.recallhub.reminder;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReminderMapper extends BaseMapper<ReminderEntity> {
    @Update("UPDATE reminders SET status='CANCELLED', cancelled_at=#{now}, lease_until=NULL, next_attempt_at=NULL " +
            "WHERE entry_id=#{entryId} AND status IN ('SCHEDULED','PROCESSING')")
    int cancelScheduledByEntry(@Param("entryId") long entryId, @Param("now") LocalDateTime now);

    @Select("SELECT id FROM reminders WHERE status='SCHEDULED' AND remind_at <= #{now} " +
            "AND (next_attempt_at IS NULL OR next_attempt_at <= #{now}) ORDER BY remind_at LIMIT #{limit}")
    List<Long> findDueIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("UPDATE reminders SET status='PROCESSING', lease_until=#{leaseUntil}, attempt_count=attempt_count+1 " +
            "WHERE id=#{id} AND status='SCHEDULED' AND remind_at <= #{now} " +
            "AND (next_attempt_at IS NULL OR next_attempt_at <= #{now})")
    int claim(@Param("id") long id, @Param("now") LocalDateTime now, @Param("leaseUntil") LocalDateTime leaseUntil);

    @Update("UPDATE reminders SET status='SCHEDULED', lease_until=NULL WHERE status='PROCESSING' AND lease_until < #{now}")
    int recoverExpired(@Param("now") LocalDateTime now);

    @Select("SELECT * FROM reminders WHERE entry_id=#{entryId} ORDER BY created_at DESC, id DESC LIMIT 1")
    ReminderEntity findLatestByEntryId(@Param("entryId") long entryId);
}
