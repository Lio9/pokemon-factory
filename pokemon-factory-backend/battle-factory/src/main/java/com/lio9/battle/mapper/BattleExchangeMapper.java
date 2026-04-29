package com.lio9.battle.mapper;



import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交换奖励记录 Mapper。
 * <p>
 * 用于记录玩家胜利后把对手宝可梦换入自己队伍时的交换明细。
 * </p>
 */
@Mapper
public interface BattleExchangeMapper {
    /**
     * 新增一次交换记录。
     */
    @Insert("INSERT INTO battle_exchange(battle_id, player_team_id, opponent_team_id, replaced_index, replaced_pokemon_json, new_pokemon_json) VALUES(#{battleId}, #{playerTeamId}, #{opponentTeamId}, #{replacedIndex}, #{replacedPokemonJson}, #{newPokemonJson})")
    void insertExchange(@Param("battleId") Long battleId, @Param("playerTeamId") Integer playerTeamId, @Param("opponentTeamId") Integer opponentTeamId, @Param("replacedIndex") Integer replacedIndex, @Param("replacedPokemonJson") String replacedPokemonJson, @Param("newPokemonJson") String newPokemonJson);
}
