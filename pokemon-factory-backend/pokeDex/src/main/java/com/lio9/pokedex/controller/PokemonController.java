package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Pokemon;
import com.lio9.common.model.Move;
import com.lio9.common.service.PokemonService;
import com.lio9.common.vo.PokemonDetailVO;
import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PokemonController.class);

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
        long startTime = System.currentTimeMillis();
        logger.info("获取宝可梦列表 - 参数: current={}, size={}, name={}", current, size, name);
        
        Page<Pokemon> page = new Page<>(current, size);
        
        // 使用新的方法
        Page<Pokemon> pokemonPage = pokemonService.searchPokemon(name, page);
        
        long endTime = System.currentTimeMillis();
        logger.info("获取宝可梦列表成功 - 耗时: {}ms, 总数: {}", (endTime - startTime), pokemonPage.getTotal());
        
        return ResultResponse.buildPageSuccess(pokemonPage);
    }
    
    /**
     * 获取宝可梦详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getPokemonDetail(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        logger.info("获取宝可梦详情 - ID: {}", id);
        
        PokemonDetailVO pokemon = pokemonService.getDetailById(id);

        long endTime = System.currentTimeMillis();
        if (pokemon != null) {
            logger.info("获取宝可梦详情成功 - 耗时: {}ms", (endTime - startTime));
            return ResultResponse.buildSuccess("success", pokemon);
        } else {
            logger.warn("获取宝可梦详情失败 - 找不到ID为{}的宝可梦, 耗时: {}ms", id, (endTime - startTime));
            return ResultResponse.buildNotFound("宝可梦", id);
        }
    }
    
    /**
     * 获取宝可梦技能列表
     */
    @GetMapping("/{id}/moves")
    public Map<String, Object> getPokemonMoves(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        logger.info("获取宝可梦技能列表 - ID: {}", id);
        
        try {
            List<Move> moves = pokemonService.getMoves(id);
            long endTime = System.currentTimeMillis();
            logger.info("获取宝可梦技能列表成功 - 耗时: {}ms, 技能数量: {}", (endTime - startTime), moves.size());
            return ResultResponse.buildSuccess("success", moves);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            logger.error("获取宝可梦技能列表失败 - ID: {}, 耗时: {}ms, 错误: {}", id, (endTime - startTime), e.getMessage());
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
        long startTime = System.currentTimeMillis();
        logger.info("搜索宝可梦 - 关键词: {}, current={}, size={}", keyword, current, size);
        
        Page<Pokemon> page = new Page<>(current, size);
        Page<Pokemon> pokemonPage = pokemonService.searchPokemon(keyword, page);
        
        long endTime = System.currentTimeMillis();
        logger.info("搜索宝可梦成功 - 耗时: {}ms, 总数: {}", (endTime - startTime), pokemonPage.getTotal());
        
        return ResultResponse.buildPageSuccess(pokemonPage);
    }
    
    /**
     * 根据编号获取宝可梦
     */
    @GetMapping("/number/{indexNumber}")
    public Map<String, Object> getPokemonByIndexNumber(@PathVariable String indexNumber) {
        long startTime = System.currentTimeMillis();
        logger.info("根据编号获取宝可梦 - 编号: {}", indexNumber);
        
        Pokemon pokemon = pokemonService.getByIndexNumber(indexNumber);
        
        long endTime = System.currentTimeMillis();
        if (pokemon != null) {
            logger.info("根据编号获取宝可梦成功 - 耗时: {}ms", (endTime - startTime));
            return ResultResponse.buildSuccess("success", pokemon);
        } else {
            logger.warn("根据编号获取宝可梦失败 - 找不到编号为{}的宝可梦, 耗时: {}ms", indexNumber, (endTime - startTime));
            return ResultResponse.buildNotFound("宝可梦", indexNumber);
        }
    }
    
    /**
     * 获取进化链
     */
    @GetMapping("/{id}/evolution")
    public Map<String, Object> getEvolutionChain(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        logger.info("获取进化链 - ID: {}", id);
        
        try {
            Object evolutionChain = pokemonService.getEvolutionChain(id);
            long endTime = System.currentTimeMillis();
            logger.info("获取进化链成功 - 耗时: {}ms", (endTime - startTime));
            return ResultResponse.buildSuccess("success", evolutionChain);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            logger.error("获取进化链失败 - ID: {}, 耗时: {}ms, 错误: {}", id, (endTime - startTime), e.getMessage());
            return ResultResponse.buildError("获取进化链失败", e.getMessage());
        }
    }
}