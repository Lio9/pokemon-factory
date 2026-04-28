package com.lio9.battle.mapper;



/**
 * PlayerMapper 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 对战工厂玩家进度 Mapper。
 */
@Mapper
public interface PlayerMapper {
    /**
     * 如果玩家不存在则插入一条初始记录。
     */
    @Insert("INSERT OR IGNORE INTO player(username, rank, points, tier, tier_points, total_points, highest_tier, wins, losses) VALUES(#{username}, 0, 0, 0, 0, 0, 0, 0, 0)")
    void insertIgnore(@Param("username") String username);

    /**
     * 按用户名查询玩家 ID。
     */
    @Select("SELECT id FROM player WHERE username = #{username}")
    Integer findIdByUsername(@Param("username") String username);

    /**
     * 按用户名查询玩家完整进度信息。
     */
    @Select("SELECT id, username, rank, points, tier, tier_points, total_points, highest_tier, wins, losses, tier_reached_at, created_at FROM player WHERE username = #{username}")
    Map<String, Object> findByUsername(@Param("username") String username);

    /**
     * 按 ID 查询玩家完整进度信息。
     */
    @Select("SELECT id, username, rank, points, tier, tier_points, total_points, highest_tier, wins, losses, tier_reached_at, created_at FROM player WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") Integer id);

    /**
     * 更新玩家积分和段位（兼容旧逻辑）。
     */
    @Update("UPDATE player SET rank = #{rank}, points = #{points} WHERE id = #{playerId}")
    void updateProgress(@Param("playerId") Integer playerId, @Param("rank") Integer rank, @Param("points") Integer points);

    /**
     * 更新玩家段位系统的完整进度。
     */
    @Update("UPDATE player SET tier = #{tier}, tier_points = #{tierPoints}, total_points = #{totalPoints}, highest_tier = #{highestTier}, wins = #{wins}, losses = #{losses}, tier_reached_at = CASE WHEN #{tier} > tier THEN datetime('now') ELSE tier_reached_at END WHERE id = #{playerId}")
    void updateTierProgress(@Param("playerId") Integer playerId, @Param("tier") Integer tier, @Param("tierPoints") Integer tierPoints, @Param("totalPoints") Integer totalPoints, @Param("highestTier") Integer highestTier, @Param("wins") Integer wins, @Param("losses") Integer losses);

    /**
     * 大师球段位排行榜。
     */
    @Select("SELECT username, total_points, tier, wins, losses, tier_reached_at FROM player WHERE tier >= 3 ORDER BY total_points DESC, tier_reached_at ASC LIMIT #{limit}")
    List<Map<String, Object>> leaderboard(@Param("limit") Integer limit);

    /**
     * 查询玩家的对战历史。
     */
    @Select("SELECT b.id, b.started_at, b.ended_at, b.rounds, b.battle_phase, b.winner_player_id, b.factory_run_id, b.run_battle_number FROM battle b WHERE b.player_id = #{playerId} ORDER BY b.id DESC LIMIT #{limit}")
    List<Map<String, Object>> findBattleHistory(@Param("playerId") Integer playerId, @Param("limit") Integer limit);
}
