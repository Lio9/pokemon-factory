package com.lio9.pokedex.mapper;



/**
 * ItemMapper 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 物品Mapper接口
 */
@Mapper
public interface ItemMapper extends BaseMapper<Item> {
    
    /**
     * 搜索物品
     */
    @Select("SELECT * FROM item WHERE name LIKE CONCAT('%', #{keyword}, '%') OR name_en LIKE CONCAT('%', #{keyword}, '%') ORDER BY id")
    List<Item> searchItems(@Param("keyword") String keyword);
}
