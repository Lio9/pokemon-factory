package com.lio9.pokedex.mapper;



/**
 * PokemonStatsMapper 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.PokemonStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 能力值Mapper接口
 */
@Mapper
public interface PokemonStatsMapper extends BaseMapper<PokemonStats> {
}
