package com.lio9.pokedex.controller;



/**
 * PokedexController 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端控制器文件。
 * 核心职责：负责承接 HTTP 请求、整理参数并调用业务层返回统一响应。
 * 阅读建议：建议先看接口入口方法，再追踪到 service 层。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.response.ResultResponse;
import com.lio9.pokedex.service.PokedexService;
import com.lio9.pokedex.vo.AbilityVO;
import com.lio9.pokedex.vo.ItemVO;
import com.lio9.pokedex.vo.MoveVO;
import com.lio9.pokedex.vo.PokemonDetailVO;
import com.lio9.pokedex.vo.PokemonListVO;
import com.lio9.pokedex.vo.TypeVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 图鉴控制器
 */
@RestController
@RequestMapping("/api/pokedex")
public class PokedexController {
    private final PokedexService pokedexService;

    public PokedexController(PokedexService pokedexService) {
        this.pokedexService = pokedexService;
    }

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
