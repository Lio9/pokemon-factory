package com.lio9.battle.mapper;



/**
 * OpponentPoolMapper 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 对手池 Mapper。
 */
@Mapper
public interface OpponentPoolMapper {
    /**
     * 新增一条对手池候选队伍。
     */
    @Insert("INSERT INTO opponent_pool(team_id, rank, created_at) VALUES(#{teamId}, #{rank}, datetime('now'))")
    void addTeam(@Param("teamId") Integer teamId, @Param("rank") Integer rank);

    /**
     * 按目标段位附近抽样候选队伍。
     */
    @Select("SELECT op.id, op.team_id AS team_id, t.team_json, op.rank, ABS(op.rank - #{targetRank}) AS rank_gap FROM opponent_pool op JOIN team t ON op.team_id = t.id WHERE op.rank BETWEEN #{low} AND #{high} ORDER BY rank_gap ASC, RANDOM() LIMIT #{limit}")
    List<Map<String,Object>> sample(@Param("low") int low, @Param("high") int high, @Param("limit") int limit, @Param("targetRank") int targetRank);

    /**
     * 清理过旧的对手池记录。
     */
    @Delete("DELETE FROM opponent_pool WHERE created_at < datetime('now', #{offset})")
    void cleanupOlderThan(@Param("offset") String offset);
}
