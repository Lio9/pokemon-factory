package com.lio9.pokedex.vo;



import lombok.Data;

import java.util.List;

/**
 * 宝可梦形态VO
 * 创建人: Lio9
 */
@Data
public class PokemonFormVO {
    
    private Long id;
    
    private String name;
    
    private String indexNumber;
    
    private Boolean isMega;
    
    private Boolean isGmax;
    
    private String image;
    
    private Integer hp;
    
    private Integer attack;
    
    private Integer defense;
    
    private Integer spAttack;
    
    private Integer spDefense;
    
    private Integer speed;
    
    private List<TypeVO> types;
    
    private List<AbilityVO> abilities;
    
    private List<MoveVO> moves;
}