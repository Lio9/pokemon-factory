package com.lio9.pokedex.controller;

import com.lio9.pokedex.service.PokeapiDataService;
import com.lio9.pokedex.service.EfficientImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 优化的导入控制器
 * 提供更快速的批量导入功能
 */
@RestController
@RequestMapping("/api/import-optimized")
@CrossOrigin(origins = "*")
public class OptimizedImportController {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedImportController.class);

    @Autowired
    private PokeapiDataService pokeapiDataService;
    
    @Autowired
    private EfficientImportService efficientImportService;

    /**
     * 快速导入所有数据（优化版）
     */
    @PostMapping("/all-fast")
    public ResponseEntity<Map<String, Object>> fastImportAllData() {
        Map<String, Object> result = new HashMap<>();
        String taskId = "FAST-IMPORT-" + System.currentTimeMillis();

        try {
            logger.info("启动快速导入任务，任务ID: {}", taskId);

            // 异步执行快速导入
            CompletableFuture.runAsync(() -> {
                try {
                    // 使用优化器进行批量导入
                    pokeapiDataService.importAllPokemonDataOptimized();
                    
                    logger.info("快速导入完成");
                    
                } catch (Exception e) {
                    logger.error("快速导入失败: {}", e.getMessage(), e);
                }
            });

            result.put("code", 200);
            result.put("message", "快速导入任务已启动");
            result.put("data", Map.of(
                "taskId", taskId,
                "status", "running",
                "message", "正在执行优化导入..."
            ));

        } catch (Exception e) {
            logger.error("启动快速导入任务失败: {}", e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "启动快速导入任务失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 使用高效混合导入方案导入所有数据
     * 结合Python异步网络请求和Java数据库操作的优势
     */
    @PostMapping("/efficient-import")
    public ResponseEntity<Map<String, Object>> efficientImportAllData() {
        Map<String, Object> result = new HashMap<>();
        String taskId = "EFFICIENT-IMPORT-" + System.currentTimeMillis();
        
        try {
            logger.info("启动高效混合导入任务，任务ID: {}", taskId);
            
            // 异步执行导入任务，避免超时
            CompletableFuture.runAsync(() -> {
                try {
                    // 调用EfficientImportService执行导入
                    Map<String, Object> importResult = efficientImportService.callEfficientPythonImport();
                    
                    if ((Boolean) importResult.get("success")) {
                        logger.info("高效混合导入任务 {} 执行成功", taskId);
                    } else {
                        logger.error("高效混合导入任务 {} 执行失败: {}", taskId, importResult.get("error"));
                    }
                } catch (Exception e) {
                    logger.error("高效混合导入任务 {} 执行异常: {}", taskId, e.getMessage(), e);
                }
            });
            
            result.put("code", 200);
            result.put("message", "高效混合导入任务已启动");
            result.put("data", Map.of(
                "taskId", taskId,
                "statusUrl", "/api/import-optimized/import-status/" + taskId,
                "status", "running",
                "message", "正在执行高效混合导入..."
            ));
            
        } catch (Exception e) {
            logger.error("启动高效混合导入任务失败: {}", e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "启动高效混合导入任务失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取高效导入任务状态
     */
    @GetMapping("/import-status/{taskId}")
    public ResponseEntity<Map<String, Object>> getImportStatus(@PathVariable String taskId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> status = efficientImportService.getImportStatus(taskId);
            result.put("code", 200);
            result.put("message", "获取状态成功");
            result.put("data", status);
        } catch (Exception e) {
            logger.error("获取导入状态失败: {}", e.getMessage());
            result.put("code", 500);
            result.put("message", "获取导入状态失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}