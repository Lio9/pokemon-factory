package com.lio9.pokedex.service;



/**
 * PokemonService 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端业务服务文件。
 * 核心职责：负责定义或承载模块级业务能力，对上层暴露稳定服务接口。
 * 阅读建议：建议结合控制器和实现类一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
