package com.lio9.battle.mapper;



/**
 * BattleExchangeMapper 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
