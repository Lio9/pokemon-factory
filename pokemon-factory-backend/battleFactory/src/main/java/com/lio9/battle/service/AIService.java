package com.lio9.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.PokemonMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AIService {
    private final PokemonMapper pokemonMapper;
    private final ObjectMapper mapper = new ObjectMapper();

    public AIService(PokemonMapper pokemonMapper) {
        this.pokemonMapper = pokemonMapper;
    }

    // Generate a simple opponent team (JSON) by sampling random pokemons
    public String generateOpponentTeamJson(int size) {
        List<Map<String,Object>> sample = pokemonMapper.sampleLimit(size);
        try {
            return mapper.writeValueAsString(sample);
        } catch (Exception e) {
            return "[]";
        }
    }
}
