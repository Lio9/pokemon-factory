package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.service.PokedexService;
import com.lio9.common.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
        
        Map<String, Object> result = new HashMap<>();
        try {
            Page<PokemonListVO> page = pokedexService.getPokemonList(current, size, typeId, generationId, keyword);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", page);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取宝可梦详情
     */
    @GetMapping("/pokemon/{id}")
    public Map<String, Object> getPokemonDetail(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            PokemonDetailVO detail = pokedexService.getPokemonDetail(id);
            if (detail != null) {
                result.put("code", 200);
                result.put("message", "success");
                result.put("data", detail);
            } else {
                result.put("code", 404);
                result.put("message", "宝可梦不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取形态技能列表
     */
    @GetMapping("/form/{formId}/moves")
    public Map<String, Object> getFormMoves(
            @PathVariable Integer formId,
            @RequestParam(required = false) Integer versionGroupId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<MoveVO> moves = pokedexService.getFormMoves(formId, versionGroupId);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", moves);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取所有属性
     */
    @GetMapping("/types")
    public Map<String, Object> getAllTypes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<TypeVO> types = pokedexService.getAllTypes();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", types);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取特性列表
     */
    @GetMapping("/abilities/list")
    public Map<String, Object> getAbilityList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            Page<AbilityVO> page = pokedexService.getAbilityList(current, size, keyword);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", page);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
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
        
        Map<String, Object> result = new HashMap<>();
        try {
            Page<MoveVO> page = pokedexService.getMoveList(current, size, typeId, keyword);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", page);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
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
        
        Map<String, Object> result = new HashMap<>();
        try {
            Page<ItemVO> page = pokedexService.getItemList(current, size, categoryId, keyword);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", page);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
}
