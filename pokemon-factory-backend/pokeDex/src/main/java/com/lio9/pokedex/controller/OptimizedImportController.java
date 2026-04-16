package com.lio9.pokedex.controller;

import com.lio9.pokedex.service.PokeapiDataService;
import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
import com.lio9.common.response.ImportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 优化的导入控制器
 * <p>
 * 提供图鉴导入相关接口，支持异步处理、进度监控和状态查询。
 * <p>
 * 支持的导入方式：
 * - 快速导入：优化的 Java 导入流程
 * - 异步导入：非阻塞的后台导入任务
 * <p>
 * 主要功能：
 * - 导入任务管理（启动、状态查询、进度监控）
 * - 多种导入类型支持（pokemon、move、item等）
 * - 性能统计和监控
 * - 错误处理和恢复机制
 *
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/import-optimized")
public class OptimizedImportController {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedImportController.class);
    private final PokeapiDataService pokeapiDataService;

    public OptimizedImportController(PokeapiDataService pokeapiDataService) {
        this.pokeapiDataService = pokeapiDataService;
    }


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
                    Map<String, Object> importResult = pokeapiDataService.importAllPokemonDataOptimized();

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

                    ImportResponse.buildImportTaskResponse(taskId, "快速导入任务已启动", "optimized")

            );


        } catch (Exception e) {

            logger.error("启动快速导入任务失败: {}", e.getMessage(), e);

            return ResponseEntity.ok(

                    ImportResponse.buildImportErrorResponse(taskId, "启动快速导入任务失败: " + e.getMessage())

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

                    ImportResponse.buildImportTaskResponse(taskId, "异步导入任务已启动", "async")

            );


        } catch (Exception e) {

            logger.error("启动异步导入任务失败: {}", e.getMessage(), e);

            return ResponseEntity.ok(

                    ImportResponse.buildImportErrorResponse(taskId, "启动异步导入任务失败: " + e.getMessage())

            );

        }
    }

    /**
     * 获取性能监控数据
     */
    @GetMapping("/performance-stats")
    public ResponseEntity<Map<String, Object>> getPerformanceStats() {
        try {
            Map<String, Object> stats = pokeapiDataService.getPerformanceStats();
            return ResponseEntity.ok(ResultResponse.buildSuccess("获取性能统计成功", stats));
        } catch (Exception e) {
            logger.error("获取性能统计失败: {}", e.getMessage());
            return ResponseEntity.ok(ResultResponse.buildError("获取性能统计失败", e.getMessage()));
        }
    }

    /**
     * 获取高效导入任务状态
     */
    @GetMapping("/import-status/{taskId}")
    public ResponseEntity<Map<String, Object>> getImportStatus(@PathVariable String taskId) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("taskId", taskId);
            if (taskId.startsWith("ASYNC-")) {
                status.put("status", "running");
                status.put("message", "异步导入任务进行中");
                status.put("importType", "async");
            } else {
                status.put("status", "running");
                status.put("message", "快速导入任务进行中");
                status.put("importType", "optimized");
            }
            status.put("progress", pokeapiDataService.getImportProgressStatus());
            return ResponseEntity.ok(ResultResponse.buildSuccess("获取状态成功", status));
        } catch (Exception e) {
            logger.error("获取导入状态失败: {}", e.getMessage());
            return ResponseEntity.ok(ResultResponse.buildError("获取导入状态失败", e.getMessage()));
        }
    }
}
