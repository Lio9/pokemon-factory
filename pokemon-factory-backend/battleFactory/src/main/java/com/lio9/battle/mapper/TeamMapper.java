package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface TeamMapper {
    @Insert("INSERT INTO team(player_id, name, team_json, source, created_at) VALUES(#{playerId}, #{name}, #{teamJson}, #{source}, datetime('now'))")
    void insertTeam(@Param("playerId") Integer playerId, @Param("name") String name, @Param("teamJson") String teamJson, @Param("source") String source);

    @Select("SELECT last_insert_rowid()")
    Integer lastInsertId();

    @Select("SELECT id, team_json, COALESCE(version,0) AS version FROM team WHERE player_id = #{playerId} ORDER BY created_at DESC LIMIT 1")
    Map<String,Object> findLatestByPlayer(@Param("playerId") Integer playerId);

    @Select("SELECT team_json FROM team WHERE id = #{id}")
    String findTeamJsonById(@Param("id") Integer id);

    @Update("UPDATE team SET team_json = #{teamJson}, version = version + 1 WHERE id = #{id} AND COALESCE(version,0) = #{expectedVersion}")
    int updateTeamWithVersion(@Param("id") Integer id, @Param("teamJson") String teamJson, @Param("expectedVersion") Integer expectedVersion);
}
