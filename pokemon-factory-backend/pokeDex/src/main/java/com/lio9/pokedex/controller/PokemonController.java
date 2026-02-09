package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Pokemon;
import com.lio9.common.service.PokemonService;
import com.lio9.common.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
        Page<Pokemon> pokemonPage = pokemonService.getPokemonPage(page, queryVO);
        
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
        PokemonDetailVO detail = pokemonService.getPokemonDetail(id);
        
        if (detail != null) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", detail);
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
        Page<Pokemon> pokemonPage = pokemonService.searchPokemon(keyword, page);
        
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
        Pokemon pokemon = pokemonService.getByIndexNumber(indexNumber);
        
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
     * 获取宝可梦进化链
     */
    @GetMapping("/{id}/evolution")
    public Map<String, Object> getEvolutionChain(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        List<EvolutionVO> evolutionChain = pokemonService.getEvolutionChain(id);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", evolutionChain);
        return result;
    }
    
    /**
     * 转换为详情VO
     */
    private PokemonDetailVO convertToDetailVO(Pokemon pokemon) {
        if (pokemon == null) return null;
        PokemonDetailVO detail = new PokemonDetailVO();
        // 简单映射，后续完善
        return detail;
    }
}
