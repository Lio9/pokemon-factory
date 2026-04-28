package com.lio9.pokedex.model;



/**
 * PokemonDetailDTO 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：领域模型文件。
 * 核心职责：负责表达数据库实体、核心领域对象或计算过程中的数据结构。
 * 阅读建议：建议关注字段语义与上下游使用方式。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 宝可梦详情DTO
 * 创建人: Lio9
 */
@Data
public class PokemonDetailDTO {
    private Long id;
    private String indexNumber;
    private String name;
    private String nameEn;
    private String nameJp;
    private String profile;
    private String genus;
    private List<PokemonForm> forms;
    private List<Map<String, Object>> evolutionChain;
}