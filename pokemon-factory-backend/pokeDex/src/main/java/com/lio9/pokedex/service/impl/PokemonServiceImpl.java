package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.PokemonMapper;
import com.lio9.common.model.Pokemon;
import com.lio9.common.service.PokemonService;
import com.lio9.common.vo.EvolutionVO;
import com.lio9.common.vo.PokemonDetailVO;
import com.lio9.common.vo.PokemonQueryVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 宝可梦服务实现类
 */
@Service
public class PokemonServiceImpl extends ServiceImpl<PokemonMapper, Pokemon> implements PokemonService {
    
    @Override
    public Page<Pokemon> getPokemonPage(Page<Pokemon> page, PokemonQueryVO vo) {
        // 简化实现
        return this.page(page);
    }
    
    @Override
    public PokemonDetailVO getPokemonDetail(Long id) {
        // 简化实现
        return new PokemonDetailVO();
    }
    
    @Override
    public Page<Pokemon> searchPokemon(String keyword, Page<Pokemon> page) {
        // 简化实现
        return this.page(page);
    }
    
    @Override
    public Pokemon getByIndexNumber(String indexNumber) {
        // 简化实现
        return null;
    }
    
    @Override
    public List<EvolutionVO> getEvolutionChain(Long pokemonId) {
        // 简化实现
        return List.of();
    }
}