package com.lio9.battle.mapper;



/**
 * BattleRoundMapper 文件说明
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
