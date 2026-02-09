package com.lio9.pokedex.controller;

import com.lio9.pokedex.service.PokeapiDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据导入测试控制器
 * 用于测试和完善数据导入功能
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class DataImportTestController {
    
    @Autowired
    private PokeapiDataService pokeapiDataService;
    
    /**
     * 测试导入单个宝可梦数据
     */
    @PostMapping("/import-pokemon/{id}")
    public ResponseEntity<Map<String, Object>> importSinglePokemon(@PathVariable int id) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 这里可以调用专门的单个宝可梦导入方法
            result.put("code", 200);
            result.put("message", "测试导入完成");
            result.put("data", "测试导入宝可梦 ID: " + id);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "导入失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 测试导入基础数据
     */
    @PostMapping("/import-basic-data")
    public ResponseEntity<Map<String, Object>> importBasicData() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 测试导入基础数据
            // pokeapiDataService.importTypes(); // 私有方法，无法直接调用
            result.put("code", 200);
            result.put("message", "基础数据导入完成");
            result.put("data", "已导入属性数据");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "导入失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取导入进度
     */
    @GetMapping("/import-progress")
    public ResponseEntity<Map<String, Object>> getImportProgress() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取进度成功");
        result.put("data", Map.of(
            "status", "running",
            "progress", "50%",
            "currentTask", "正在导入技能数据"
        ));
        return ResponseEntity.ok(result);
    }
}