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
     * 使用新的Python调度脚本导入所有数据
     */
    @PostMapping("/python-scheduler")
    public ResponseEntity<Map<String, Object>> pythonSchedulerImport() {
        Map<String, Object> result = new HashMap<>();
        String taskId = "PYTHON-SCHEDULER-" + System.currentTimeMillis();
        
        try {
            logger.info("启动Python调度导入任务，任务ID: {}", taskId);
            
            // 异步执行导入任务，避免超时
            CompletableFuture.runAsync(() -> {
                try {
                    // 调用EfficientImportService执行新的Python调度脚本
                    Map<String, Object> importResult = efficientImportService.callPythonSchedulerImport();
                    
                    if ((Boolean) importResult.get("success")) {
                        logger.info("Python调度导入任务 {} 执行成功", taskId);
                    } else {
                        logger.error("Python调度导入任务 {} 执行失败: {}", taskId, importResult.get("error"));
                    }
                } catch (Exception e) {
                    logger.error("Python调度导入任务 {} 执行异常: {}", taskId, e.getMessage(), e);
                }
            });
            
            result.put("code", 200);
            result.put("message", "Python调度导入任务已启动");
            result.put("data", Map.of(
                "taskId", taskId,
                "statusUrl", "/api/import-optimized/import-status/" + taskId,
                "status", "running",
                "message", "正在执行Python调度导入..."
            ));
            
        } catch (Exception e) {
            logger.error("启动Python调度导入任务失败: {}", e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "启动Python调度导入任务失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 使用Python调度脚本导入特定类型数据
     */
    @PostMapping("/python-scheduler/{type}")
    public ResponseEntity<Map<String, Object>> pythonSchedulerImportType(@PathVariable String type) {
        Map<String, Object> result = new HashMap<>();
        String taskId = "PYTHON-SCHEDULER-" + type.toUpperCase() + "-" + System.currentTimeMillis();
        
        try {
            logger.info("启动Python调度导入任务，任务ID: {}, 类型: {}", taskId, type);
            
            // 异步执行导入任务，避免超时
            CompletableFuture.runAsync(() -> {
                try {
                    // 调用EfficientImportService执行新的Python调度脚本
                    Map<String, Object> importResult = efficientImportService.callPythonSchedulerImportType(type);
                    
                    if ((Boolean) importResult.get("success")) {
                        logger.info("Python调度导入任务 {} 执行成功", taskId);
                    } else {
                        logger.error("Python调度导入任务 {} 执行失败: {}", taskId, importResult.get("error"));
                    }
                } catch (Exception e) {
                    logger.error("Python调度导入任务 {} 执行异常: {}", taskId, e.getMessage(), e);
                }
            });
            
            result.put("code", 200);
            result.put("message", "Python调度导入任务已启动");
            result.put("data", Map.of(
                "taskId", taskId,
                "statusUrl", "/api/import-optimized/import-status/" + taskId,
                "status", "running",
                "message", "正在执行Python调度导入: " + type
            ));
            
        } catch (Exception e) {
            logger.error("启动Python调度导入任务失败: {}", e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "启动Python调度导入任务失败: " + e.getMessage());
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