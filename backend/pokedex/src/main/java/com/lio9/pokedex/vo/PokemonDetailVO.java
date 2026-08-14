package com.lio9.pokedex.vo;

import lombok.Data;

import java.util.List;

/**
 * 宝可梦详情VO
 */
@Data
public class PokemonDetailVO {
    private Integer id;
    private String name;
    private String nameEn;
    private String nameJp;
    private String genus;
    private String description;
    private Integer generationId;
    private Boolean isLegendary;
    private Boolean isMythical;
    private Boolean isBaby;
    private Integer captureRate;
    private Integer baseHappiness;
    private Integer genderRate;
    private Integer hatchCounter;
    private String growthRate;
    private List<String> eggGroups;
    private List<PokemonFormDetailVO> forms;

    /** 进化链（扁平列表，前端 EvolutionChainPanel 直接渲染） */
    private List<EvolutionVO> evolutionChain;
}
