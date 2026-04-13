package com.lio9.pokedex.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.pokedex.vo.AbilityVO;
import com.lio9.pokedex.vo.ItemVO;
import com.lio9.pokedex.vo.MoveVO;
import com.lio9.pokedex.vo.PokemonDetailVO;
import com.lio9.pokedex.vo.PokemonListVO;
import com.lio9.pokedex.vo.TypeVO;
import java.util.List;

/**
 * 图鉴服务接口
 */
public interface PokedexService {
    
    /**
     * 获取宝可梦列表
     */
    Page<PokemonListVO> getPokemonList(int current, int size, Integer typeId, Integer generationId, String keyword);
    
    /**
     * 获取宝可梦详情
     */
    PokemonDetailVO getPokemonDetail(Integer speciesId);
    
    /**
     * 获取形态可学技能
     */
    List<MoveVO> getFormMoves(Integer formId, Integer versionGroupId);
    
    /**
     * 获取属性列表
     */
    List<TypeVO> getAllTypes();
    
    /**
     * 获取特性列表
     */
    Page<AbilityVO> getAbilityList(int current, int size, String keyword);
    
    /**
     * 获取技能列表
     */
    Page<MoveVO> getMoveList(int current, int size, Integer typeId, String keyword);
    
    /**
     * 获取物品列表
     */
    Page<ItemVO> getItemList(int current, int size, Integer categoryId, String keyword);
}
