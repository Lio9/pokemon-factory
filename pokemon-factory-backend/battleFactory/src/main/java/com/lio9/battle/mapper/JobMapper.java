package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface JobMapper {
    @Insert("INSERT INTO battle_job(battle_id, status, payload, created_at) VALUES(#{battleId}, #{status}, #{payload}, datetime('now'))")
    void insertJob(@Param("battleId") Integer battleId, @Param("status") String status, @Param("payload") String payload);

    @Select("SELECT id, battle_id, status, payload, created_at, updated_at FROM battle_job WHERE status = 'PENDING' OR status = 'RUNNING'")
    List<Map<String,Object>> findPendingJobs();

    @Update("UPDATE battle_job SET status = #{status}, updated_at = datetime('now') WHERE id = #{id}")
    void updateJobStatus(@Param("id") Integer id, @Param("status") String status);
}
