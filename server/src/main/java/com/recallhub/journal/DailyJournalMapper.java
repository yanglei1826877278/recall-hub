package com.recallhub.journal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyJournalMapper extends BaseMapper<DailyJournalEntity> {
    @Select("""
        <script>
        SELECT id, journal_date, generated_content,
               COALESCE(NULLIF(content, ''), generated_content) AS content,
               user_edited, generated_at, created_at, updated_at
        FROM daily_journals
        WHERE COALESCE(NULLIF(content, ''), generated_content) IS NOT NULL
          AND INSTR(COALESCE(NULLIF(content, ''), generated_content), #{query}) &gt; 0
        <if test='from != null'> AND journal_date &gt;= #{from}</if>
        <if test='to != null'> AND journal_date &lt;= #{to}</if>
        ORDER BY journal_date DESC, updated_at DESC
        LIMIT #{offset}, #{size}
        </script>
        """)
    List<DailyJournalEntity> search(@Param("query") String query,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to,
                                    @Param("offset") long offset,
                                    @Param("size") int size);

    @Select("""
        <script>
        SELECT COUNT(*) FROM daily_journals
        WHERE COALESCE(NULLIF(content, ''), generated_content) IS NOT NULL
          AND INSTR(COALESCE(NULLIF(content, ''), generated_content), #{query}) &gt; 0
        <if test='from != null'> AND journal_date &gt;= #{from}</if>
        <if test='to != null'> AND journal_date &lt;= #{to}</if>
        </script>
        """)
    long searchCount(@Param("query") String query,
                     @Param("from") LocalDate from,
                     @Param("to") LocalDate to);
}
