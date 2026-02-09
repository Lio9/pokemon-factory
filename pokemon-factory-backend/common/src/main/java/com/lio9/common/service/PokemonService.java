package com.lio9.common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lio9.common.model.Pokemon;
import com.lio9.common.vo.EvolutionVO;
import com.lio9.common.vo.PokemonDetailVO;
import com.lio9.common.vo.PokemonQueryVO;

import java.util.List;

/**
 * 宝可梦服务接口
 * 创建人: Lio9
 */
public interface PokemonService extends IService<Pokemon> {
    
    /**
     * 分页查询宝可梦列表
     */
    Page<Pokemon> getPokemonPage(Page<Pokemon> page, PokemonQueryVO vo);
    
    /**
     * 获取宝可梦详情
     */
    PokemonDetailVO getPokemonDetail(Long id);
    
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
}