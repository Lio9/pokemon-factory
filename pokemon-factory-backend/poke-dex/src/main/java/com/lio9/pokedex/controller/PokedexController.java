package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.response.ResultResponse;
import com.lio9.pokedex.exception.ResourceNotFoundException;
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

@RestController
@RequestMapping("/api/pokedex")
public class PokedexController {
    private final PokedexService pokedexService;

    public PokedexController(PokedexService pokedexService) {
        this.pokedexService = pokedexService;
    }

    @GetMapping("/pokemon/list")
    public Map<String, Object> getPokemonList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer typeId,
            @RequestParam(required = false) Integer generationId,
            @RequestParam(required = false) String keyword) {
        Page<PokemonListVO> page = pokedexService.getPokemonList(current, size, typeId, generationId, keyword);
        return ResultResponse.buildPageSuccess(page);
    }

    @GetMapping("/pokemon/{id}")
    public Map<String, Object> getPokemonDetail(@PathVariable Integer id) {
        PokemonDetailVO detail = pokedexService.getPokemonDetail(id);
        if (detail == null) throw new ResourceNotFoundException("宝可梦", id);
        return ResultResponse.buildSuccess("success", detail);
    }

    @GetMapping("/form/{formId}/moves")
    public Map<String, Object> getFormMoves(
            @PathVariable Integer formId,
            @RequestParam(required = false) Integer versionGroupId) {
        List<MoveVO> moves = pokedexService.getFormMoves(formId, versionGroupId);
        return ResultResponse.buildSuccess("success", moves);
    }

    @GetMapping("/types")
    public Map<String, Object> getAllTypes() {
        List<TypeVO> types = pokedexService.getAllTypes();
        return ResultResponse.buildSuccess("success", types);
    }

    @GetMapping("/abilities/list")
    public Map<String, Object> getAbilityList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<AbilityVO> page = pokedexService.getAbilityList(current, size, keyword);
        return ResultResponse.buildPageSuccess(page);
    }

    @GetMapping("/moves/list")
    public Map<String, Object> getMoveList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer typeId,
            @RequestParam(required = false) String keyword) {
        Page<MoveVO> page = pokedexService.getMoveList(current, size, typeId, keyword);
        return ResultResponse.buildPageSuccess(page);
    }

    @GetMapping("/items/list")
    public Map<String, Object> getItemList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword) {
        Page<ItemVO> page = pokedexService.getItemList(current, size, categoryId, keyword);
        return ResultResponse.buildPageSuccess(page);
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = pokedexService.getSummary();
        return ResultResponse.buildSuccess(summary);
    }
}
