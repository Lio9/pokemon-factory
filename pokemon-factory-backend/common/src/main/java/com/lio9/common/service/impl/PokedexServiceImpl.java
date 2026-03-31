package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.service.PokedexService;
import com.lio9.common.vo.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 图鉴服务实现
 */
@Service
public class PokedexServiceImpl implements PokedexService {

    @Override
    public Page<PokemonListVO> getPokemonList(int current, int size, Integer typeId, Integer generationId, String keyword) {
        // TODO: 实现获取宝可梦列表的逻辑
        return new Page<>(current, size);
    }

    @Override
    public PokemonDetailVO getPokemonDetail(Integer speciesId) {
        // TODO: 实现获取宝可梦详情的逻辑
        return new PokemonDetailVO();
    }

    @Override
    public List<MoveVO> getFormMoves(Integer formId, Integer versionGroupId) {
        // TODO: 实现获取形态技能的逻辑
        return List.of();
    }

    @Override
    public List<TypeVO> getAllTypes() {
        // TODO: 实现获取属性列表的逻辑
        return List.of();
    }

    @Override
    public Page<AbilityVO> getAbilityList(int current, int size, String keyword) {
        // TODO: 实现获取特性列表的逻辑
        return new Page<>(current, size);
    }

    @Override
    public Page<MoveVO> getMoveList(int current, int size, Integer typeId, String keyword) {
        // TODO: 实现获取技能列表的逻辑
        return new Page<>(current, size);
    }

    @Override
    public Page<ItemVO> getItemList(int current, int size, Integer categoryId, String keyword) {
        // TODO: 实现获取物品列表的逻辑
        return new Page<>(current, size);
    }
}