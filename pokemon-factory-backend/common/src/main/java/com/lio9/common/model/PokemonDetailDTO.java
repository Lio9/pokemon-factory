package com.lio9.common.model;

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