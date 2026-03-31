package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Pokemon;
import com.lio9.common.model.Move;
import com.lio9.common.service.PokemonService;
import com.lio9.common.vo.PokemonDetailVO;
import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
        Page<Pokemon> page = new Page<>(current, size);
        
        // 使用新的方法
        Page<Pokemon> pokemonPage = pokemonService.searchPokemon(name, page);
        
        return ResultResponse.buildPageSuccess(pokemonPage);
    }
    
    /**
     * 获取宝可梦详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getPokemonDetail(@PathVariable Long id) {
        PokemonDetailVO pokemon = pokemonService.getDetailById(id);

        if (pokemon != null) {
            return ResultResponse.buildSuccess("success", pokemon);
        } else {
            return ResultResponse.buildNotFound("宝可梦", id);
        }
    }
    
    /**
     * 获取宝可梦技能列表
     */
    @GetMapping("/{id}/moves")
    public Map<String, Object> getPokemonMoves(@PathVariable Long id) {
        try {
            List<Move> moves = pokemonService.getMoves(id);
            return ResultResponse.buildSuccess("success", moves);
        } catch (Exception e) {
            return ResultResponse.buildError("获取技能失败", e.getMessage());
        }
    }
    
    /**
     * 搜索宝可梦
     */
    @GetMapping("/search")
    public Map<String, Object> searchPokemon(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        
        Page<Pokemon> page = new Page<>(current, size);
        Page<Pokemon> pokemonPage = pokemonService.searchPokemon(keyword, page);
        
        return ResultResponse.buildPageSuccess(pokemonPage);
    }
    
    /**
     * 根据编号获取宝可梦
     */
    @GetMapping("/number/{indexNumber}")
    public Map<String, Object> getPokemonByIndexNumber(@PathVariable String indexNumber) {
        Pokemon pokemon = pokemonService.getByIndexNumber(indexNumber);
        
        if (pokemon != null) {
            return ResultResponse.buildSuccess("success", pokemon);
        } else {
            return ResultResponse.buildNotFound("宝可梦", indexNumber);
        }
    }
    
    /**
     * 获取进化链
     */
    @GetMapping("/{id}/evolution")
    public Map<String, Object> getEvolutionChain(@PathVariable Long id) {
        try {
            return ResultResponse.buildSuccess("success", pokemonService.getEvolutionChain(id));
        } catch (Exception e) {
            return ResultResponse.buildError("获取进化链失败", e.getMessage());
        }
    }
}