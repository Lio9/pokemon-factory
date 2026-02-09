package com.lio9.pokedex.controller;

import com.lio9.pokedex.service.PokeapiDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * PokeAPI数据控制器
 * 用于从PokeAPI获取完整宝可梦数据
 */
@RestController
@RequestMapping("/api/pokeapi")
@CrossOrigin(origins = "*")
public class PokeapiDataController {
    
    @Autowired
    private PokeapiDataService pokeapiDataService;
    
    /**
     * 从PokeAPI获取所有宝可梦数据并导入数据库
     */
    @PostMapping("/import-all")
    public ResponseEntity<Map<String, Object>> importAllPokemonData() {
        Map<String, Object> result = new HashMap<>();
        String importResult = pokeapiDataService.importAllPokemonData();
        result.put("code", 200);
        result.put("message", "导入完成");
        result.put("data", importResult);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 清空所有表数据
     */
    @PostMapping("/clear-all")
    public ResponseEntity<Map<String, Object>> clearAllTables() {
        Map<String, Object> result = new HashMap<>();
        String clearResult = pokeapiDataService.clearAllData();
        result.put("code", 200);
        result.put("message", "清空数据完成");
        result.put("data", clearResult);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取导入状态
     */
    @GetMapping("/import-status")
    public ResponseEntity<Map<String, Object>> getImportStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取导入状态成功");
        result.put("data", Map.of(
            "status", "ready",
            "message", "等待导入命令"
        ));
        return ResponseEntity.ok(result);
    }
}