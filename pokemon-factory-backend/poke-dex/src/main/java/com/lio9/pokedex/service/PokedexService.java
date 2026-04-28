package com.lio9.pokedex.service;



/**
 * PokedexService 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端业务服务文件。
 * 核心职责：负责定义或承载模块级业务能力，对上层暴露稳定服务接口。
 * 阅读建议：建议结合控制器和实现类一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
