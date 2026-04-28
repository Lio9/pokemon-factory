package com.lio9.battle.mapper;



/**
 * PokemonMapper 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * battleFactory 侧的轻量宝可梦抽样 Mapper。
 */
@Component("battlePokemonMapper")
@Mapper
public interface PokemonMapper {
    /**
     * 随机抽样指定数量的宝可梦。
     */
    @Select("SELECT id, name, base_experience FROM pokemon ORDER BY RANDOM() LIMIT #{limit}")
    List<Map<String,Object>> sampleLimit(@Param("limit") int limit);

    /**
     * 按基础经验范围抽样宝可梦。
     */
    @Select("SELECT id, name, base_experience FROM pokemon WHERE base_experience BETWEEN #{minBaseExperience} AND #{maxBaseExperience} ORDER BY RANDOM() LIMIT #{limit}")
    List<Map<String,Object>> sampleByBaseExperience(@Param("minBaseExperience") int minBaseExperience, @Param("maxBaseExperience") int maxBaseExperience, @Param("limit") int limit);
}
