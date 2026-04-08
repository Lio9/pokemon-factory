package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface BattleMapper {
    @Insert("INSERT INTO battle(player_id, opponent_team_id, rounds, player_move_map, player_team_json, battle_phase, started_at) VALUES(#{playerId}, #{opponentTeamId}, #{rounds}, #{playerMoveMapJson}, #{playerTeamJson}, #{battlePhase}, datetime('now'))")
    void insertInitial(@Param("playerId") Integer playerId, @Param("opponentTeamId") Integer opponentTeamId, @Param("rounds") Integer rounds, @Param("playerMoveMapJson") String playerMoveMapJson, @Param("playerTeamJson") String playerTeamJson, @Param("battlePhase") String battlePhase);

    @Insert("INSERT INTO battle(player_id, opponent_team_id, rounds, summary_json, battle_phase, started_at, ended_at, winner_player_id) VALUES(#{playerId}, #{opponentTeamId}, #{rounds}, #{summaryJson}, #{battlePhase}, datetime('now'), datetime('now'), #{winnerPlayerId})")
    void insertFinal(@Param("playerId") Integer playerId, @Param("opponentTeamId") Integer opponentTeamId, @Param("rounds") Integer rounds, @Param("summaryJson") String summaryJson, @Param("winnerPlayerId") Integer winnerPlayerId, @Param("battlePhase") String battlePhase);

    @Select("SELECT last_insert_rowid()")
    Integer lastInsertId();

    @Select("SELECT b.id, b.player_id, b.opponent_team_id, b.player_move_map, b.player_team_json, b.battle_phase, b.started_at, b.ended_at, b.summary_json, t.team_json AS opponent_team_json FROM battle b LEFT JOIN team t ON b.opponent_team_id = t.id WHERE b.id = #{id}")
    Map<String,Object> findBattleWithOpponent(@Param("id") Long id);

    @Update("UPDATE battle SET opponent_team_id = #{opponentTeamId}, summary_json = #{summaryJson}, rounds = #{rounds}, battle_phase = #{battlePhase} WHERE id = #{id}")
    void updateBattleState(@Param("id") Integer id, @Param("opponentTeamId") Integer opponentTeamId, @Param("summaryJson") String summaryJson, @Param("rounds") Integer rounds, @Param("battlePhase") String battlePhase);

    @Update("UPDATE battle SET opponent_team_id = #{opponentTeamId}, summary_json = #{summaryJson}, rounds = #{rounds}, battle_phase = #{battlePhase}, ended_at = datetime('now'), winner_player_id = #{winnerPlayerId} WHERE id = #{id}")
    void updateBattle(@Param("id") Integer id, @Param("opponentTeamId") Integer opponentTeamId, @Param("summaryJson") String summaryJson, @Param("rounds") Integer rounds, @Param("winnerPlayerId") Integer winnerPlayerId, @Param("battlePhase") String battlePhase);

    @Update("UPDATE battle SET player_team_json = #{playerTeamJson}, summary_json = #{summaryJson}, battle_phase = #{battlePhase} WHERE id = #{id}")
    void updateBattleTeamState(@Param("id") Integer id, @Param("playerTeamJson") String playerTeamJson, @Param("summaryJson") String summaryJson, @Param("battlePhase") String battlePhase);
}
