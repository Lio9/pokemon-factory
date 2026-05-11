package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.pokedex.exception.ResourceNotFoundException;
import com.lio9.pokedex.model.Pokemon;
import com.lio9.pokedex.model.Move;
import com.lio9.common.response.ResultResponse;
import com.lio9.pokedex.service.PokemonService;
import com.lio9.pokedex.vo.PokemonDetailVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pokemon")
public class PokemonController {

    private static final Logger log = LoggerFactory.getLogger(PokemonController.class);
    private final PokemonService pokemonService;

    public PokemonController(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    @GetMapping("/list")
    public Map<String, Object> getPokemonList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String name) {
        Page<Pokemon> pokemonPage = pokemonService.searchPokemon(name, new Page<>(current, size));
        return ResultResponse.buildPageSuccess(pokemonPage);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getPokemonDetail(@PathVariable Long id) {
        PokemonDetailVO pokemon = pokemonService.getDetailById(id);
        if (pokemon == null) throw new ResourceNotFoundException("宝可梦", id);
        return ResultResponse.buildSuccess("success", pokemon);
    }

    @GetMapping("/{id}/moves")
    public Map<String, Object> getPokemonMoves(@PathVariable Long id) {
        List<Move> moves = pokemonService.getMoves(id);
        return ResultResponse.buildSuccess("success", moves);
    }

    @GetMapping("/search")
    public Map<String, Object> searchPokemon(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        Page<Pokemon> pokemonPage = pokemonService.searchPokemon(keyword, new Page<>(current, size));
        return ResultResponse.buildPageSuccess(pokemonPage);
    }

    @GetMapping("/number/{indexNumber}")
    public Map<String, Object> getPokemonByIndexNumber(@PathVariable String indexNumber) {
        Pokemon pokemon = pokemonService.getByIndexNumber(indexNumber);
        if (pokemon == null) throw new ResourceNotFoundException("宝可梦", indexNumber);
        return ResultResponse.buildSuccess("success", pokemon);
    }

    @GetMapping("/{id}/evolution")
    public Map<String, Object> getEvolutionChain(@PathVariable Long id) {
        Object evolutionChain = pokemonService.getEvolutionChain(id);
        return ResultResponse.buildSuccess("success", evolutionChain);
    }
}
