package com.lio9.pokedex.controller;

import com.lio9.common.model.Item;
import com.lio9.common.model.Pokemon;
import com.lio9.common.service.ItemService;
import com.lio9.common.service.PokemonService;
import com.lio9.pokedex.service.PokeapiDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一数据导入控制器
 * 整合所有数据导入功能，提供统一的导入接口
 * 创建人: Lio9
 */
@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "*")
public class DataImportController {
    
    @Autowired
    private PokeapiDataService pokeapiDataService;
    
    @Autowired
    private PokemonService pokemonService;
    
    @Autowired
    private ItemService itemService;
    
    /**
     * 统一导入所有数据（从PokeAPI）
     */
    @PostMapping("/all")
    public Map<String, Object> importAllData() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> importResult = pokeapiDataService.importAllPokemonData();
            result.put("code", 200);
            result.put("message", "导入完成");
            result.put("data", importResult);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "导入失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 导入宝可梦数据
     */
    @PostMapping("/pokemon")
    public Map<String, Object> importPokemonData(@RequestParam(defaultValue = "1") Integer startId,
                                                 @RequestParam(defaultValue = "151") Integer count) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> importResult = pokeapiDataService.importPokemonRange(startId, count);
            result.put("code", 200);
            result.put("message", "宝可梦数据导入完成");
            result.put("data", importResult);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "宝可梦数据导入失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 导入技能数据
     */
    @PostMapping("/moves")
    public Map<String, Object> importMoveData() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> importResult = pokeapiDataService.importMoveData();
            result.put("code", 200);
            result.put("message", "技能数据导入完成");
            result.put("data", importResult);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "技能数据导入失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 导入物品数据
     */
    @PostMapping("/items")
    public Map<String, Object> importItemData() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> importResult = pokeapiDataService.importItemData();
            result.put("code", 200);
            result.put("message", "物品数据导入完成");
            result.put("data", importResult);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "物品数据导入失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 批量导入宝可梦数据
     */
    @PostMapping("/pokemon/batch")
    public Map<String, Object> importPokemonBatch(@RequestBody List<Pokemon> pokemonList) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (pokemonList == null || pokemonList.isEmpty()) {
                result.put("code", 400);
                result.put("message", "导入数据不能为空");
                return result;
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (Pokemon pokemon : pokemonList) {
                try {
                    if (pokemonService.save(pokemon)) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                }
            }
            
            result.put("code", 200);
            result.put("message", "批量导入完成");
            result.put("data", Map.of(
                "total", pokemonList.size(),
                "success", successCount,
                "fail", failCount
            ));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "批量导入失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 批量导入物品数据
     */
    @PostMapping("/items/batch")
    public Map<String, Object> importItemsBatch(@RequestBody List<Item> items) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (items == null || items.isEmpty()) {
                result.put("code", 400);
                result.put("message", "导入数据不能为空");
                return result;
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (Item item : items) {
                try {
                    if (itemService.save(item)) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                }
            }
            
            result.put("code", 200);
            result.put("message", "批量导入完成");
            result.put("data", Map.of(
                "total", items.size(),
                "success", successCount,
                "fail", failCount
            ));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "批量导入失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 清空所有数据
     */
    @DeleteMapping("/all")
    public Map<String, Object> clearAllData() {
        Map<String, Object> result = new HashMap<>();
        try {
            pokeapiDataService.clearAllData();
            result.put("code", 200);
            result.put("message", "数据清空完成");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "数据清空失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 清空指定表数据
     */
    @DeleteMapping("/table/{tableName}")
    public Map<String, Object> clearTableData(@PathVariable String tableName) {
        Map<String, Object> result = new HashMap<>();
        try {
            pokeapiDataService.clearTableData(tableName);
            result.put("code", 200);
            result.put("message", tableName + "数据清空完成");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", tableName + "数据清空失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取导入状态
     */
    @GetMapping("/status")
    public Map<String, Object> getImportStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> status = new HashMap<>();
            
            status.put("pokemonCount", pokemonService.count());
            status.put("itemCount", itemService.count());
            
            result.put("code", 200);
            result.put("message", "状态获取成功");
            result.put("data", status);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "状态获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 从文件导入数据
     */
    @PostMapping("/from-file")
    public Map<String, Object> importFromFile(@RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "pokemon") String type) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file.isEmpty()) {
                result.put("code", 400);
                result.put("message", "文件不能为空");
                return result;
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                result.put("code", 400);
                result.put("message", "文件名无效");
                return result;
            }
            
            result.put("code", 200);
            result.put("message", "文件接收成功，待处理");
            result.put("data", Map.of(
                "fileName", fileName,
                "type", type,
                "size", file.getSize()
            ));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "文件处理失败: " + e.getMessage());
        }
        return result;
    }
}