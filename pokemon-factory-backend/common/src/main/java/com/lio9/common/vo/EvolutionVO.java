package com.lio9.common.vo;

import lombok.Data;

/**
 * 进化链VO
 * 创建人: Lio9
 */
@Data
public class EvolutionVO {
    
    private Long id;
    
    private Long pokemonId;
    
    private String pokemonName;
    
    private String pokemonIndexNumber;
    
    private Long evolvesFromId;
    
    private String evolvesFromName;
    
    private String evolutionMethod;
    
    private String evolutionParameter;
    
    private String evolutionValue;
}