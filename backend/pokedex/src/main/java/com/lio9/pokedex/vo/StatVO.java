package com.lio9.pokedex.vo;



import lombok.Data;

/**
 * 种族值VO
 */
@Data
public class StatVO {
    private Integer hp;
    private Integer attack;
    private Integer defense;
    private Integer spAttack;
    private Integer spDefense;
    private Integer speed;
    private Integer total;
}