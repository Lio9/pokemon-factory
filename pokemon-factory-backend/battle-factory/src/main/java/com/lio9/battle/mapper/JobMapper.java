package com.lio9.battle.mapper;



/**
 * JobMapper 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 异步 battle_job 任务表 Mapper。
 */
@Mapper
public interface JobMapper {
    /**
     * 新增一条异步任务。
     */
    @Insert("INSERT INTO battle_job(battle_id, status, payload, created_at) VALUES(#{battleId}, #{status}, #{payload}, datetime('now'))")
    void insertJob(@Param("battleId") Integer battleId, @Param("status") String status, @Param("payload") String payload);

    /**
     * 查询仍需执行或恢复的任务。
     */
    @Select("SELECT id, battle_id, status, payload, created_at, updated_at FROM battle_job WHERE status = 'PENDING' OR status = 'RUNNING'")
    List<Map<String,Object>> findPendingJobs();

    /**
     * 更新任务状态。
     */
    @Update("UPDATE battle_job SET status = #{status}, updated_at = datetime('now') WHERE id = #{id}")
    void updateJobStatus(@Param("id") Integer id, @Param("status") String status);

    /**
     * 按 battleId 直接更新异步任务状态，避免扫描全表。
     */
    @Update("UPDATE battle_job SET status = #{status}, updated_at = datetime('now') WHERE battle_id = #{battleId} AND (status = 'PENDING' OR status = 'RUNNING')")
    void updateJobStatusByBattleId(@Param("battleId") Integer battleId, @Param("status") String status);
}
