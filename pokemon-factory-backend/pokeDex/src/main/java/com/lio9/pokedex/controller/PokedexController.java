package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.response.ResultResponse;
import com.lio9.pokedex.service.PokedexService;
import com.lio9.pokedex.vo.AbilityVO;
import com.lio9.pokedex.vo.ItemVO;
import com.lio9.pokedex.vo.MoveVO;
import com.lio9.pokedex.vo.PokemonDetailVO;
import com.lio9.pokedex.vo.PokemonListVO;
import com.lio9.pokedex.vo.TypeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 图鉴控制器
 */
@RestController
@RequestMapping("/api/pokedex")
@CrossOrigin(origins = "*")
public class PokedexController {

    @Autowired
    private PokedexService pokedexService;

    /**
     * 获取宝可梦列表
     */
    @GetMapping("/pokemon/list")
    public Map<String, Object> getPokemonList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer typeId,
            @RequestParam(required = false) Integer generationId,
            @RequestParam(required = false) String keyword) {
        
        try {
            Page<PokemonListVO> page = pokedexService.getPokemonList(current, size, typeId, generationId, keyword);
            return ResultResponse.buildPageSuccess(page);
        } catch (Exception e) {
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }

    /**
     * 获取宝可梦详情
     */
    @GetMapping("/pokemon/{id}")
    public Map<String, Object> getPokemonDetail(@PathVariable Integer id) {
        try {
            PokemonDetailVO detail = pokedexService.getPokemonDetail(id);
            if (detail != null) {
                return ResultResponse.buildSuccess("success", detail);
            } else {
                return ResultResponse.buildNotFound("宝可梦", id);
            }
        } catch (Exception e) {
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }

    /**
     * 获取形态技能列表
     */
    @GetMapping("/form/{formId}/moves")
    public Map<String, Object> getFormMoves(
            @PathVariable Integer formId,
            @RequestParam(required = false) Integer versionGroupId) {
        try {
            List<MoveVO> moves = pokedexService.getFormMoves(formId, versionGroupId);
            return ResultResponse.buildSuccess("success", moves);
        } catch (Exception e) {
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }

    /**
     * 获取所有属性
     */
    @GetMapping("/types")
    public Map<String, Object> getAllTypes() {
        try {
            List<TypeVO> types = pokedexService.getAllTypes();
            return ResultResponse.buildSuccess("success", types);
        } catch (Exception e) {
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }

    /**
     * 获取特性列表
     */
    @GetMapping("/abilities/list")
    public Map<String, Object> getAbilityList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        
        try {
            Page<AbilityVO> page = pokedexService.getAbilityList(current, size, keyword);
            return ResultResponse.buildPageSuccess(page);
        } catch (Exception e) {
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }

    /**
     * 获取技能列表
     */
    @GetMapping("/moves/list")
    public Map<String, Object> getMoveList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer typeId,
            @RequestParam(required = false) String keyword) {
        
        try {
            Page<MoveVO> page = pokedexService.getMoveList(current, size, typeId, keyword);
            return ResultResponse.buildPageSuccess(page);
        } catch (Exception e) {
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }

    /**
     * 获取物品列表
     */
    @GetMapping("/items/list")
    public Map<String, Object> getItemList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword) {
        
        try {
            Page<ItemVO> page = pokedexService.getItemList(current, size, categoryId, keyword);
            return ResultResponse.buildPageSuccess(page);
        } catch (Exception e) {
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }
}
