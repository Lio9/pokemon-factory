package com.lio9.pokedex.service.pokemon;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Pokemon;
import com.lio9.common.vo.PokemonDetailVO;
import com.lio9.common.vo.EvolutionChainVO;
import com.lio9.common.vo.MoveVO;
import java.util.List;

/**
 * 宝可梦服务
 * 负责宝可梦相关的所有业务逻辑
 */
public interface PokemonService {
    
    /**
     * 分页查询宝可梦列表
     */
    Page<Pokemon> getSpeciesList(Integer current, Integer size, String name);
    
    /**
     * 根据ID获取宝可梦详情
     */
    PokemonDetailVO getSpeciesDetail(Integer speciesId);
    
    /**
     * 获取宝可梦进化链
     */
    List<EvolutionChainVO> getEvolutionChain(Long speciesId);
    
    /**
     * 获取宝可梦可学技能
     */
    List<MoveVO> getSpeciesMoves(Long speciesId);
    
    /**
     * 搜索宝可梦
     */
    Page<Pokemon> searchSpecies(String keyword, Page<Pokemon> page);
    
    /**
     * 根据编号获取宝可梦
     */
    Pokemon getByIndexNumber(String indexNumber);
    
    /**
     * 统计宝可梦数量
     */
    long count();
    
    /**
     * 获取宝可梦形态详情
     */
    PokemonDetailVO getFormDetail(Integer formId);
    
    /**
     * 获取形态技能
     */
    List<MoveVO> getFormMoves(Integer formId, Integer versionGroupId);
}