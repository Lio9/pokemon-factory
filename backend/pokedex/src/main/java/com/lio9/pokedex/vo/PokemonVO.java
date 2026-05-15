package com.lio9.pokedex.vo;



import lombok.Data;

import java.util.List;

/**
 * 宝可梦响应VO
 */
@Data
public class PokemonVO {
    
    private Long id;
    
    private String indexNumber;
    
    private String name;
    
    private String nameEn;
    
    private String nameJp;
    
    private String profile;
    
    private String genus;
    
    private List<PokemonFormVO> forms;
    
    private List<EvolutionVO> evolutions;
    
    private List<EggGroupVO> eggGroups;
}