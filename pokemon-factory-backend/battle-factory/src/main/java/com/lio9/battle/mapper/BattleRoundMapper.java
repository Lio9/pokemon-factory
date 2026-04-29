package com.lio9.battle.mapper;



import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

/**
 * battle_round 回合日志 Mapper。
 */
@Mapper
public interface BattleRoundMapper {
    /**
     * 插入单个回合日志。
     */
    @Insert("INSERT INTO battle_round(battle_id, round_number, log_json) VALUES(#{battleId}, #{roundNumber}, #{logJson})")
    void insertRound(@Param("battleId") Integer battleId, @Param("roundNumber") Integer roundNumber, @Param("logJson") String logJson);
}
