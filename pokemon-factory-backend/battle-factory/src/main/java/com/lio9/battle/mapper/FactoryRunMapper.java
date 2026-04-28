package com.lio9.battle.mapper;



/**
 * FactoryRunMapper 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 工厂挑战（连续 9 轮对战）Mapper。
 */
@Mapper
public interface FactoryRunMapper {

    @Insert("INSERT INTO factory_run(player_id, max_battles, team_json, tier_at_start, status) VALUES(#{playerId}, #{maxBattles}, #{teamJson}, #{tierAtStart}, 'active')")
    void insertRun(@Param("playerId") Integer playerId, @Param("maxBattles") Integer maxBattles, @Param("teamJson") String teamJson, @Param("tierAtStart") Integer tierAtStart);

    @Select("SELECT last_insert_rowid()")
    Integer lastInsertId();

    @Select("SELECT * FROM factory_run WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") Integer id);

    @Select("SELECT * FROM factory_run WHERE player_id = #{playerId} AND status = 'active' ORDER BY id DESC LIMIT 1")
    Map<String, Object> findActiveRun(@Param("playerId") Integer playerId);

    @Update("UPDATE factory_run SET current_battle = #{currentBattle}, wins = #{wins}, losses = #{losses}, current_battle_id = #{currentBattleId}, team_json = #{teamJson} WHERE id = #{id}")
    void updateProgress(@Param("id") Integer id, @Param("currentBattle") Integer currentBattle, @Param("wins") Integer wins, @Param("losses") Integer losses, @Param("currentBattleId") Integer currentBattleId, @Param("teamJson") String teamJson);

    @Update("UPDATE factory_run SET status = #{status}, points_earned = #{pointsEarned}, ended_at = datetime('now') WHERE id = #{id}")
    void finishRun(@Param("id") Integer id, @Param("status") String status, @Param("pointsEarned") Integer pointsEarned);

    @Select("SELECT * FROM factory_run WHERE player_id = #{playerId} ORDER BY id DESC LIMIT #{limit}")
    List<Map<String, Object>> findRecentRuns(@Param("playerId") Integer playerId, @Param("limit") Integer limit);
}
