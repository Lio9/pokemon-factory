package com.lio9.battle.mapper;



import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * battle 主表 Mapper。
 * <p>
 * 负责对战主记录的创建、状态更新、结束结算和状态查询。
 * </p>
 */
@Mapper
public interface BattleMapper {
    /**
     * 插入一条初始 battle 记录。
     */
    @Insert("INSERT INTO battle(player_id, opponent_team_id, rounds, player_move_map, player_team_json, battle_phase, factory_run_id, run_battle_number, started_at) VALUES(#{playerId}, #{opponentTeamId}, #{rounds}, #{playerMoveMapJson}, #{playerTeamJson}, #{battlePhase}, #{factoryRunId}, #{runBattleNumber}, datetime('now'))")
    void insertInitial(@Param("playerId") Integer playerId, @Param("opponentTeamId") Integer opponentTeamId, @Param("rounds") Integer rounds, @Param("playerMoveMapJson") String playerMoveMapJson, @Param("playerTeamJson") String playerTeamJson, @Param("battlePhase") String battlePhase, @Param("factoryRunId") Integer factoryRunId, @Param("runBattleNumber") Integer runBattleNumber);

    /**
     * 插入一条已结束的 battle 记录。
     */
    @Insert("INSERT INTO battle(player_id, opponent_team_id, rounds, summary_json, battle_phase, started_at, ended_at, winner_player_id) VALUES(#{playerId}, #{opponentTeamId}, #{rounds}, #{summaryJson}, #{battlePhase}, datetime('now'), datetime('now'), #{winnerPlayerId})")
    void insertFinal(@Param("playerId") Integer playerId, @Param("opponentTeamId") Integer opponentTeamId, @Param("rounds") Integer rounds, @Param("summaryJson") String summaryJson, @Param("winnerPlayerId") Integer winnerPlayerId, @Param("battlePhase") String battlePhase);

    /**
     * 读取 SQLite 最近一次插入的主键。
     */
    @Select("SELECT last_insert_rowid()")
    Integer lastInsertId();

    /**
     * 查询 battle 主记录及对手队伍 JSON。
     */
    @Select("SELECT b.id, b.player_id, b.opponent_team_id, b.player_move_map, b.player_team_json, b.battle_phase, b.started_at, b.ended_at, b.summary_json, t.team_json AS opponent_team_json FROM battle b LEFT JOIN team t ON b.opponent_team_id = t.id WHERE b.id = #{id}")
    Map<String,Object> findBattleWithOpponent(@Param("id") Long id);

    /**
     * 更新 battle 进行中的摘要状态。
     */
    @Update("UPDATE battle SET opponent_team_id = #{opponentTeamId}, summary_json = #{summaryJson}, rounds = #{rounds}, battle_phase = #{battlePhase} WHERE id = #{id}")
    void updateBattleState(@Param("id") Integer id, @Param("opponentTeamId") Integer opponentTeamId, @Param("summaryJson") String summaryJson, @Param("rounds") Integer rounds, @Param("battlePhase") String battlePhase);

    /**
     * 更新 battle 结束态信息。
     */
    @Update("UPDATE battle SET opponent_team_id = #{opponentTeamId}, summary_json = #{summaryJson}, rounds = #{rounds}, battle_phase = #{battlePhase}, ended_at = datetime('now'), winner_player_id = #{winnerPlayerId} WHERE id = #{id}")
    void updateBattle(@Param("id") Integer id, @Param("opponentTeamId") Integer opponentTeamId, @Param("summaryJson") String summaryJson, @Param("rounds") Integer rounds, @Param("winnerPlayerId") Integer winnerPlayerId, @Param("battlePhase") String battlePhase);

    /**
     * 只更新玩家队伍快照和摘要状态。
     */
    @Update("UPDATE battle SET player_team_json = #{playerTeamJson}, summary_json = #{summaryJson}, battle_phase = #{battlePhase} WHERE id = #{id}")
    void updateBattleTeamState(@Param("id") Integer id, @Param("playerTeamJson") String playerTeamJson, @Param("summaryJson") String summaryJson, @Param("battlePhase") String battlePhase);
}
