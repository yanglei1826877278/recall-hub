package com.recallhub.entry;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EntryMapper extends BaseMapper<EntryEntity> {
    @Select("""
        <script>
        SELECT * FROM entries
        WHERE deleted_at IS NULL
          AND MATCH(title, content) AGAINST(#{query} IN BOOLEAN MODE)
        <if test='type != null and type != ""'> AND type = #{type}</if>
        <if test='status != null and status != ""'> AND status = #{status}</if>
        <if test='from != null'> AND COALESCE(occurred_at, created_at) &gt;= #{from}</if>
        <if test='to != null'> AND COALESCE(occurred_at, created_at) &lt; #{to}</if>
        ORDER BY MATCH(title, content) AGAINST(#{query} IN BOOLEAN MODE) DESC, created_at DESC
        LIMIT #{offset}, #{size}
        </script>
        """)
    List<EntryEntity> fullTextSearch(@Param("query") String query, @Param("type") String type,
                                     @Param("status") String status, @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to, @Param("offset") long offset,
                                     @Param("size") int size);

    @Select("""
        <script>
        SELECT COUNT(*) FROM entries
        WHERE deleted_at IS NULL
          AND MATCH(title, content) AGAINST(#{query} IN BOOLEAN MODE)
        <if test='type != null and type != ""'> AND type = #{type}</if>
        <if test='status != null and status != ""'> AND status = #{status}</if>
        <if test='from != null'> AND COALESCE(occurred_at, created_at) &gt;= #{from}</if>
        <if test='to != null'> AND COALESCE(occurred_at, created_at) &lt; #{to}</if>
        </script>
        """)
    long fullTextCount(@Param("query") String query, @Param("type") String type,
                       @Param("status") String status, @Param("from") LocalDateTime from,
                       @Param("to") LocalDateTime to);

    @Select("SELECT * FROM entries WHERE deleted_at IS NULL AND type=#{type} " +
            "AND COALESCE(occurred_at, created_at) >= #{from} AND COALESCE(occurred_at, created_at) < #{to} " +
            "ORDER BY COALESCE(occurred_at, created_at), id")
    List<EntryEntity> byEffectiveRange(@Param("type") String type, @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    @Select("""
        <script>
        SELECT * FROM entries WHERE deleted_at IS NULL
        <if test='type != null and type != ""'> AND type=#{type}</if>
        <if test='before != null'> AND COALESCE(occurred_at, created_at) &lt; #{before}</if>
        <if test='after != null'> AND COALESCE(occurred_at, created_at) &gt; #{after}</if>
        ORDER BY COALESCE(occurred_at, created_at) DESC, id DESC LIMIT #{limit}
        </script>
        """)
    List<EntryEntity> timeline(@Param("before") LocalDateTime before, @Param("after") LocalDateTime after,
                               @Param("type") String type, @Param("limit") int limit);
}
