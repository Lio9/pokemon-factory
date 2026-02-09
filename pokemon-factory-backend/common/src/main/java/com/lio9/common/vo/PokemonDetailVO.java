package com.lio9.common.vo;

import lombok.Data;
import java.util.List;

/**
 * 宝可梦详情响应VO
 * 创建人: Lio9
 */
@Data
public class PokemonDetailVO {
    
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