package com.lio9.pokedex.controller;

import com.lio9.pokedex.service.PokeapiDataService;
import com.lio9.pokedex.service.EfficientImportService;
import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
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
 * 提供更快速的批量导入功能，支持异步处理和进度监控
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
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
    
    @Autowired
    private com.lio9.pokedex.service.impl.EfficientImportServiceImpl efficientImportServiceImpl;



    /**
     * 快速导入所有数据（优化版）
     * 异步执行批量导入，提供更好的用户体验
     * 
     * @return 导入结果状态
     */
    @PostMapping("/all-fast")
    public ResponseEntity<Map<String, Object>> fastImportAllData() {
        String taskId = "FAST-IMPORT-" + System.currentTimeMillis();

        try {
            logger.info("启动快速导入任务，任务ID: {}", taskId);

            // 异步执行快速导入
            CompletableFuture.runAsync(() -> {
                try {
                    // 调用EfficientImportService执行新的Python调度脚本
                    Map<String, Object> importResult = efficientImportService.callPythonSchedulerImport();
                    
                    if ((Boolean) importResult.get("success")) {
                        logger.info("快速导入完成");
                    } else {
                        logger.error("快速导入失败: {}", importResult.get("error"));
                    }
                } catch (Exception e) {
                    logger.error("快速导入失败: {}", e.getMessage(), e);
                }
            });

                                    return ResponseEntity.ok(

                                        ResultResponse.buildImportTaskResponse(taskId, "快速导入任务已启动", "optimized")

                                    );

                        

                                } catch (Exception e) {

                                    logger.error("启动快速导入任务失败: {}", e.getMessage(), e);

                                    return ResponseEntity.ok(

                                        ResultResponse.buildImportErrorResponse(taskId, "启动快速导入任务失败: " + e.getMessage())

                                    );

                                }
    }
    
    /**
     * 异步导入所有数据
     */
    @PostMapping("/all-async")
    public ResponseEntity<Map<String, Object>> asyncImportAllData() {
        String taskId = "ASYNC-IMPORT-" + System.currentTimeMillis();

        try {
            logger.info("启动异步导入任务，任务ID: {}", taskId);

            // 异步执行导入
            CompletableFuture<Map<String, Object>> importFuture = pokeapiDataService.importAllPokemonDataAsync();

            // 启动状态监控线程
            CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> importResult = importFuture.get();
                    logger.info("异步导入任务 {} 完成，结果: {}", taskId, importResult.get("success"));
                } catch (Exception e) {
                    logger.error("异步导入任务 {} 执行异常: {}", taskId, e.getMessage());
                }
            });

                                    return ResponseEntity.ok(

                                        ResultResponse.buildImportTaskResponse(taskId, "异步导入任务已启动", "async")

                                    );

                        

                                } catch (Exception e) {

                                    logger.error("启动异步导入任务失败: {}", e.getMessage(), e);

                                    return ResponseEntity.ok(

                                        ResultResponse.buildImportErrorResponse(taskId, "启动异步导入任务失败: " + e.getMessage())

                                    );

                                }
    }

    /**
     * 使用新的Python调度脚本导入所有数据
     */
    @PostMapping("/python-scheduler")
    public ResponseEntity<Map<String, Object>> pythonSchedulerImport() {
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
            
                                    return ResponseEntity.ok(
            
                                        ResultResponse.buildImportTaskResponse(taskId, "Python调度导入任务已启动", "python-scheduler")
            
                                    );
            
                                    
            
                                } catch (Exception e) {
            
                                    logger.error("启动Python调度导入任务失败: {}", e.getMessage(), e);
            
                                    return ResponseEntity.ok(
            
                                        ResultResponse.buildImportErrorResponse(taskId, "启动Python调度导入任务失败: " + e.getMessage())
            
                                    );
            
                                }
    }

    /**
     * 使用Python调度脚本导入特定类型数据
     */
    @PostMapping("/python-scheduler/{type}")
    public ResponseEntity<Map<String, Object>> pythonSchedulerImportType(@PathVariable String type) {
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
            
                                    return ResponseEntity.ok(
            
                                        ResultResponse.buildImportTaskResponse(taskId, "Python调度导入任务已启动", "python-scheduler")
            
                                    );
            
                                    
            
                                } catch (Exception e) {
            
                                    logger.error("启动Python调度导入任务失败: {}", e.getMessage(), e);
            
                                    return ResponseEntity.ok(
            
                                        ResultResponse.buildImportErrorResponse(taskId, "启动Python调度导入任务失败: " + e.getMessage())
            
                                    );
            
                                }
    }

    /**
     * 获取高效导入任务状态
     */
    @GetMapping("/import-status/{taskId}")
    public ResponseEntity<Map<String, Object>> getImportStatus(@PathVariable String taskId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查是否是Python调度器的任务
            if (taskId.startsWith("PYTHON-")) {
                Map<String, Object> status = efficientImportService.getImportStatus(taskId);
                result.put("code", 200);
                result.put("message", "获取状态成功");
                result.put("data", status);
            } else {
                // 检查是否是Java导入任务
                Map<String, Object> javaStatus = new HashMap<>();
                javaStatus.put("taskId", taskId);
                
                // 根据任务ID判断导入类型
                if (taskId.startsWith("JAVA-IMPORT-")) {
                    javaStatus.put("status", "running");
                    javaStatus.put("message", "Java代码直接导入任务进行中");
                    javaStatus.put("importType", "java-direct");
                } else if (taskId.startsWith("ASYNC-")) {
                    javaStatus.put("status", "running");
                    javaStatus.put("message", "异步导入任务进行中");
                    javaStatus.put("importType", "async");
                } else if (taskId.startsWith("FAST-IMPORT-")) {
                    javaStatus.put("status", "running");
                    javaStatus.put("message", "快速导入任务进行中");
                    javaStatus.put("importType", "optimized");
                } else {
                    javaStatus.put("status", "running");
                    javaStatus.put("message", "Java导入任务进行中");
                    javaStatus.put("importType", "optimized");
                }
                
                // 获取导入进度状态
                Map<String, Object> progressStatus = pokeapiDataService.getImportProgressStatus();
                javaStatus.put("progress", progressStatus);
                
                result.put("code", 200);
                result.put("message", "获取状态成功");
                result.put("data", javaStatus);
            }
        } catch (Exception e) {
            logger.error("获取导入状态失败: {}", e.getMessage());
            result.put("code", 500);
            result.put("message", "获取导入状态失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Java代码直接导入所有数据
     * 直接调用Java代码进行导入，不通过Python脚本
     */
    @PostMapping("/java-import/all")
    public ResponseEntity<Map<String, Object>> javaImportAllData() {
        String taskId = "JAVA-IMPORT-" + System.currentTimeMillis();

        try {
            logger.info("启动Java代码直接导入任务，任务ID: {}", taskId);

            // 异步执行Java导入
            CompletableFuture.runAsync(() -> {
                try {
                    // 调用EfficientImportService的Java导入实现
                    Map<String, Object> importResult = efficientImportService.callPythonSchedulerImport();
                    
                    if ((Boolean) importResult.get("success")) {
                        logger.info("Java代码直接导入完成");
                    } else {
                        logger.error("Java代码直接导入失败: {}", importResult.get("error"));
                    }
                } catch (Exception e) {
                    logger.error("Java代码直接导入失败: {}", e.getMessage(), e);
                }
            });

                                    return ResponseEntity.ok(

                                        ResultResponse.buildImportTaskResponse(taskId, "Java代码直接导入任务已启动", "java-direct")

                                    );

                        

                                } catch (Exception e) {

                                    logger.error("启动Java代码直接导入任务失败: {}", e.getMessage(), e);

                                    return ResponseEntity.ok(

                                        ResultResponse.buildImportErrorResponse(taskId, "启动Java代码直接导入任务失败: " + e.getMessage())

                                    );

                                }
    }

    /**
     * Java代码直接导入特定类型数据
     */
    @PostMapping("/java-import/{type}")
    public ResponseEntity<Map<String, Object>> javaImportType(@PathVariable String type) {
        String taskId = "JAVA-IMPORT-" + type.toUpperCase() + "-" + System.currentTimeMillis();
        
        try {
            logger.info("启动Java代码直接导入任务，任务ID: {}, 类型: {}", taskId, type);
            
            // 异步执行Java导入
            CompletableFuture.runAsync(() -> {
                try {
                    // 调用EfficientImportService的Java导入实现
                    Map<String, Object> importResult = efficientImportService.callPythonSchedulerImportType(type);
                    
                    if ((Boolean) importResult.get("success")) {
                        logger.info("Java代码直接导入任务 {} 执行成功", taskId);
                    } else {
                        logger.error("Java代码直接导入任务 {} 执行失败: {}", taskId, importResult.get("error"));
                    }
                } catch (Exception e) {
                    logger.error("Java代码直接导入任务 {} 执行异常: {}", taskId, e.getMessage(), e);
                }
            });
            
                                    return ResponseEntity.ok(
            
                                        ResultResponse.buildImportTaskResponse(taskId, "Java代码直接导入任务已启动", "java-direct")
            
                                    );
            
                                    
            
                                } catch (Exception e) {
            
                                    logger.error("启动Java代码直接导入任务失败: {}", e.getMessage(), e);
            
                                    return ResponseEntity.ok(
            
                                        ResultResponse.buildImportErrorResponse(taskId, "启动Java代码直接导入任务失败: " + e.getMessage())
            
                                    );
            
                                }
    }
}