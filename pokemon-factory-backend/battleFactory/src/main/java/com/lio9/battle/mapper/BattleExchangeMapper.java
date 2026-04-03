package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BattleExchangeMapper {
    @Insert("INSERT INTO battle_exchange(battle_id, player_team_id, opponent_team_id, replaced_index, replaced_pokemon_json, new_pokemon_json) VALUES(#{battleId}, #{playerTeamId}, #{opponentTeamId}, #{replacedIndex}, #{replacedPokemonJson}, #{newPokemonJson})")
    void insertExchange(@Param("battleId") Long battleId, @Param("playerTeamId") Integer playerTeamId, @Param("opponentTeamId") Integer opponentTeamId, @Param("replacedIndex") Integer replacedIndex, @Param("replacedPokemonJson") String replacedPokemonJson, @Param("newPokemonJson") String newPokemonJson);
}
