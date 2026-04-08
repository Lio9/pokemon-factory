package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * 对战工厂玩家进度 Mapper。
 */
@Mapper
public interface PlayerMapper {
    /**
     * 如果玩家不存在则插入一条初始记录。
     */
    @Insert("INSERT OR IGNORE INTO player(username, rank, points) VALUES(#{username}, 0, 0)")
    void insertIgnore(@Param("username") String username);

    /**
     * 按用户名查询玩家 ID。
     */
    @Select("SELECT id FROM player WHERE username = #{username}")
    Integer findIdByUsername(@Param("username") String username);

    /**
     * 按用户名查询玩家完整进度信息。
     */
    @Select("SELECT id, username, rank, points FROM player WHERE username = #{username}")
    Map<String, Object> findByUsername(@Param("username") String username);

    /**
     * 更新玩家积分和段位。
     */
    @Update("UPDATE player SET rank = #{rank}, points = #{points} WHERE id = #{playerId}")
    void updateProgress(@Param("playerId") Integer playerId, @Param("rank") Integer rank, @Param("points") Integer points);
}
