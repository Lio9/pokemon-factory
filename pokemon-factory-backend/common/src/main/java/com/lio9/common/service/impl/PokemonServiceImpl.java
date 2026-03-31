package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.PokemonMapper;
import com.lio9.common.model.Pokemon;
import com.lio9.common.model.Move;
import com.lio9.common.service.PokemonService;
import com.lio9.common.vo.EvolutionVO;
import com.lio9.common.vo.PokemonDetailVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 宝可梦服务实现
 */
@Service
public class PokemonServiceImpl extends ServiceImpl<PokemonMapper, Pokemon> implements PokemonService {

    @Override
    public long count() {
        return count();
    }

    @Override
    public void removeAll() {
        remove(null);
    }

    @Override
    public PokemonDetailVO getDetailById(Long id) {
        // 需要实现获取详情的逻辑
        return null;
    }

    @Override
    public Page<Pokemon> searchPokemon(String keyword, Page<Pokemon> page) {
        QueryWrapper<Pokemon> wrapper = new QueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("name", keyword).or().like("name_en", keyword));
        }
        
        wrapper.orderByAsc("id");
        return page(page, wrapper);
    }

    @Override
    public Pokemon getByIndexNumber(String indexNumber) {
        QueryWrapper<Pokemon> wrapper = new QueryWrapper<>();
        wrapper.eq("id", indexNumber);
        return getOne(wrapper);
    }

    @Override
    public List<EvolutionVO> getEvolutionChain(Long pokemonId) {
        // 需要实现获取进化链的逻辑
        return null;
    }

    @Override
    public List<Move> getMoves(Long pokemonId) {
        // 需要实现获取技能列表的逻辑
        return null;
    }
}