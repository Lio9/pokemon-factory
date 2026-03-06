package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Pokemon;
import com.lio9.common.model.Move;
import com.lio9.common.service.PokemonService;
import com.lio9.common.vo.PokemonDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 宝可梦控制器
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
    public Map<String, Object> getPokemonList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String name) {
        Map<String, Object> result = new HashMap<>();
        Page<Pokemon> page = new Page<>(current, size);
        
        QueryWrapper<Pokemon> queryWrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name)
                       .or()
                       .like("name_en", name);
        }
        queryWrapper.orderByAsc("id");
        
        // 使用新的方法
        Page<Pokemon> pokemonPage = pokemonService.searchPokemon(name, page);
        
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
        PokemonDetailVO pokemon = pokemonService.getDetailById(id);

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
     * 获取宝可梦技能列表
     */
    @GetMapping("/{id}/moves")
    public Map<String, Object> getPokemonMoves(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Move> moves = pokemonService.getMoves(id);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", moves);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取技能失败: " + e.getMessage());
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
     * 获取进化链
     */
    @GetMapping("/{id}/evolution")
    public Map<String, Object> getEvolutionChain(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", pokemonService.getEvolutionChain(id));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取进化链失败: " + e.getMessage());
        }
        
        return result;
    }
}