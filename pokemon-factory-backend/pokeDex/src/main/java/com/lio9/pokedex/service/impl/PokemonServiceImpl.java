package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.PokemonMapper;
import com.lio9.common.model.*;
import com.lio9.common.service.PokemonService;
import com.lio9.common.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 宝可梦服务实现类
 * 提供宝可梦数据的分页查询、搜索和管理功能
 * 继承MyBatis-Plus的ServiceImpl，实现PokemonService接口
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
 */
@Service
public class PokemonServiceImpl extends ServiceImpl<PokemonMapper, Pokemon> implements PokemonService {
    
    @Resource
    private PokemonMapper pokemonMapper;
    
    /**
     * 获取宝可梦分页列表
     * 根据分页参数和查询条件获取宝可梦数据，支持按名称模糊搜索
     * 
     * @param page 分页参数
     * @param vo 查询条件对象，包含名称搜索条件
     * @return 宝可梦分页数据
     */
    @Override
    public Page<Pokemon> getPokemonPage(Page<Pokemon> page, PokemonQueryVO vo) {
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        if (vo.getName() != null && !vo.getName().isEmpty()) {
            queryWrapper.like("name", vo.getName())
                       .or()
                       .like("name_en", vo.getName())
                       .or()
                       .like("name_jp", vo.getName());
        }
        queryWrapper.orderByAsc("id");
        return this.page(page, queryWrapper);
    }
    
    /**
     * 获取宝可梦详细信息
     * 根据宝可梦ID获取宝可梦的详细信息，包括形态、属性等关联数据
     * 
     * @param id 宝可梦ID
     * @return 宝可梦详细信息VO
     */
    @Override
    public PokemonDetailVO getPokemonDetail(Long id) {
        Pokemon pokemon = this.getById(id);
        if (pokemon == null) {
            return null;
        }
        
        PokemonDetailVO detail = new PokemonDetailVO();
        BeanUtils.copyProperties(pokemon, detail);
        
        // 查询形态信息
        QueryWrapper<PokemonForm> formWrapper = new QueryWrapper<>();
        formWrapper.eq("pokemon_id", id);
        List<PokemonForm> forms = pokemonMapper.selectPokemonForms(formWrapper);
        
        // 转换为VO
        List<PokemonFormVO> formVOs = forms.stream().map(form -> {
            PokemonFormVO formVO = new PokemonFormVO();
            BeanUtils.copyProperties(form, formVO);
            
            // 查询形态属性
            QueryWrapper<PokemonFormType> typeWrapper = new QueryWrapper<>();
            typeWrapper.eq("pokemon_form_id", form.getId());
            List<PokemonFormType> formTypes = pokemonMapper.selectPokemonFormTypes(typeWrapper);
            formVO.setTypes(formTypes.stream().map(pft -> {
                TypeVO typeVO = new TypeVO();
                Type type = new Type(); // 这里应该从数据库查询，暂时使用模拟
                type.setId(pft.getTypeId());
                type.setName("草");
                type.setColor("#78C850");
                BeanUtils.copyProperties(type, typeVO);
                return typeVO;
            }).collect(Collectors.toList()));
            
            return formVO;
        }).collect(Collectors.toList());
        
        detail.setForms(formVOs);
        
        // 查询进化链
        List<EvolutionVO> evolutionChain = getEvolutionChain(id);
        detail.setEvolutions(evolutionChain);
        
        return detail;
    }
    
    @Override
    public Page<Pokemon> searchPokemon(String keyword, Page<Pokemon> page) {
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", keyword)
                   .or()
                   .like("name_en", keyword)
                   .or()
                   .like("name_jp", keyword)
                   .orderByAsc("id");
        return this.page(page, queryWrapper);
    }
    
    @Override
    public Pokemon getByIndexNumber(String indexNumber) {
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("index_number", indexNumber);
        return this.getOne(queryWrapper);
    }
    
    @Override
    public List<EvolutionVO> getEvolutionChain(Long pokemonId) {
        QueryWrapper<EvolutionChain> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pokemon_id", pokemonId);
        List<EvolutionChain> evolutionChains = pokemonMapper.selectEvolutionChains(queryWrapper);
        
        return evolutionChains.stream().map(ec -> {
            EvolutionVO evolutionVO = new EvolutionVO();
            BeanUtils.copyProperties(ec, evolutionVO);
            return evolutionVO;
        }).collect(Collectors.toList());
    }
    
    /**
     * 获取宝可梦技能列表
     */
    public List<Move> getMoves(Long pokemonId) {
        // 查询该宝可梦的所有技能
        QueryWrapper<PokemonMove> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pokemon_id", pokemonId);
        
        List<PokemonMove> pokemonMoves = pokemonMapper.selectPokemonMoves(queryWrapper);
        
        // 获取技能ID列表
        List<Long> moveIds = pokemonMoves.stream()
            .map(PokemonMove::getMoveId)
            .collect(Collectors.toList());
        
        if (moveIds.isEmpty()) {
            return List.of();
        }
        
        // 查询技能详情
        QueryWrapper<Move> moveQueryWrapper = new QueryWrapper<>();
        moveQueryWrapper.in("id", moveIds);
        return pokemonMapper.selectMoves(moveQueryWrapper);
    }
}