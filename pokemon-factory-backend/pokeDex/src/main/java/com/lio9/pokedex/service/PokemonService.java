package com.lio9.pokedex.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.pokedex.model.Move;
import com.lio9.pokedex.model.Pokemon;
import com.lio9.pokedex.vo.EvolutionVO;
import com.lio9.pokedex.vo.PokemonDetailVO;

import java.util.List;

/**
 * 宝可梦服务接口
 */
public interface PokemonService {
    
    /**
     * 统计宝可梦数量
     */
    long count();
    
    /**
     * 删除所有宝可梦数据
     */
    void removeAll();
    
    /**
     * 获取宝可梦详情
     */
    PokemonDetailVO getDetailById(Long id);
    
    /**
     * 搜索宝可梦
     */
    Page<Pokemon> searchPokemon(String keyword, Page<Pokemon> page);
    
    /**
     * 根据编号获取宝可梦
     */
    Pokemon getByIndexNumber(String indexNumber);
    
    /**
     * 获取宝可梦进化链
     */
    List<EvolutionVO> getEvolutionChain(Long pokemonId);
    
    /**
     * 获取宝可梦技能列表
     */
    List<Move> getMoves(Long pokemonId);
}
