package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Pokemon;
import com.lio9.common.service.PokemonService;
import com.lio9.common.vo.PokemonQueryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 宝可梦控制器
 * 创建人: Lio9
 */
@RestController
@RequestMapping("/api/pokemon")
@CrossOrigin(origins = "*")
public class PokemonController {

    @Autowired
    private PokemonService pokemonService;
    
    /**
     * 分页获取宝可梦列表
     */
    @GetMapping("/list")
    public Map<String, Object> getPokemonList(PokemonQueryVO queryVO) {
        Map<String, Object> result = new HashMap<>();
        Page<Pokemon> page = new Page<>(queryVO.getCurrent(), queryVO.getSize());
        
        // 构建查询条件
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        if (queryVO.getName() != null && !queryVO.getName().isEmpty()) {
            queryWrapper.like("name", queryVO.getName())
                       .or()
                       .like("name_en", queryVO.getName())
                       .or()
                       .like("name_jp", queryVO.getName());
        }
        queryWrapper.orderByAsc("id");
        
        Page<Pokemon> pokemonPage = pokemonService.page(page, queryWrapper);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", pokemonPage);
        return result;
    }
    
    /**
     * 获取宝可梦详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getPokemonDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Pokemon pokemon = pokemonService.getById(id);

        if (pokemon != null) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", pokemon);
        } else {
            result.put("code", 404);
            result.put("message", "宝可梦不存在");
        }
        return result;
    }
    
    /**
     * 搜索宝可梦
     */
    @GetMapping("/search")
    public Map<String, Object> searchPokemon(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        
        Map<String, Object> result = new HashMap<>();
        Page<Pokemon> page = new Page<>(current, size);
        
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", keyword)
                   .or()
                   .like("name_en", keyword)
                   .or()
                   .like("name_jp", keyword)
                   .orderByAsc("id");
        
        Page<Pokemon> pokemonPage = pokemonService.page(page, queryWrapper);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", pokemonPage);
        return result;
    }
    
    /**
     * 根据编号获取宝可梦
     */
    @GetMapping("/number/{indexNumber}")
    public Map<String, Object> getPokemonByIndexNumber(@PathVariable String indexNumber) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("index_number", indexNumber);
        Pokemon pokemon = pokemonService.getOne(queryWrapper);
        
        if (pokemon != null) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", pokemon);
        } else {
            result.put("code", 404);
            result.put("message", "宝可梦不存在");
        }
        return result;
    }
    
    }
