package com.lio9.battle.mapper;



/**
 * TeamMapper 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.apache.ibatis.annotations.*;

import java.util.Map;

/**
 * 对战队伍 Mapper。
 * <p>
 * 负责玩家队伍、自动生成队伍和交换后队伍版本控制的持久化。
 * </p>
 */
@Mapper
public interface TeamMapper {
    /**
     * 新增一支队伍。
     */
    @Insert("INSERT INTO team(player_id, name, team_json, source, created_at) VALUES(#{playerId}, #{name}, #{teamJson}, #{source}, datetime('now'))")
    void insertTeam(@Param("playerId") Integer playerId, @Param("name") String name, @Param("teamJson") String teamJson, @Param("source") String source);

    /**
     * 读取最近一次插入的主键。
     */
    @Select("SELECT last_insert_rowid()")
    Integer lastInsertId();

    /**
     * 查询玩家最近保存的一支队伍。
     */
    @Select("SELECT id, team_json, COALESCE(version,0) AS version FROM team WHERE player_id = #{playerId} ORDER BY created_at DESC LIMIT 1")
    Map<String,Object> findLatestByPlayer(@Param("playerId") Integer playerId);

    /**
     * 按 ID 查询队伍 JSON。
     */
    @Select("SELECT team_json FROM team WHERE id = #{id}")
    String findTeamJsonById(@Param("id") Integer id);

    /**
     * 带版本号地更新队伍，避免交换奖励并发写覆盖。
     */
    @Update("UPDATE team SET team_json = #{teamJson}, version = version + 1 WHERE id = #{id} AND COALESCE(version,0) = #{expectedVersion}")
    int updateTeamWithVersion(@Param("id") Integer id, @Param("teamJson") String teamJson, @Param("expectedVersion") Integer expectedVersion);
}
