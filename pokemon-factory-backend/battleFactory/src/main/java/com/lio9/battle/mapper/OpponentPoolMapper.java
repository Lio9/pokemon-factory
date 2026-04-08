package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OpponentPoolMapper {
    @Insert("INSERT INTO opponent_pool(team_id, rank, created_at) VALUES(#{teamId}, #{rank}, datetime('now'))")
    void addTeam(@Param("teamId") Integer teamId, @Param("rank") Integer rank);

    @Select("SELECT op.id, op.team_id AS team_id, t.team_json, op.rank, ABS(op.rank - #{targetRank}) AS rank_gap FROM opponent_pool op JOIN team t ON op.team_id = t.id WHERE op.rank BETWEEN #{low} AND #{high} ORDER BY rank_gap ASC, RANDOM() LIMIT #{limit}")
    List<Map<String,Object>> sample(@Param("low") int low, @Param("high") int high, @Param("limit") int limit, @Param("targetRank") int targetRank);

    @Delete("DELETE FROM opponent_pool WHERE created_at < datetime('now', #{offset})")
    void cleanupOlderThan(@Param("offset") String offset);
}
