package com.lio9.battle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AIService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public AIService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Generate a simple opponent team (JSON) by sampling random pokemons
    public String generateOpponentTeamJson(int size) {
        List<Map<String,Object>> sample = jdbcTemplate.queryForList("SELECT id, name, base_experience FROM pokemon ORDER BY RANDOM() LIMIT ?", size);
        try {
            return mapper.writeValueAsString(sample);
        } catch (Exception e) {
            return "[]";
        }
    }
}
