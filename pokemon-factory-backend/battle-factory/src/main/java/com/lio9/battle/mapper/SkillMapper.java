package com.lio9.battle.mapper;



/**
 * SkillMapper 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

/**
 * 技能目录 Mapper。
 */
@Mapper
public interface SkillMapper {
    /**
     * 读取全部技能默认配置。
     */
    @Select("SELECT name, default_cooldown FROM skill_catalog")
    List<Map<String,Object>> findAll();
}
