package com.lio9.pokedex.controller;

import com.lio9.pokedex.service.PokeapiDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一数据导入控制器
 * 整合所有数据导入功能，提供统一的异步导入接口
 * 创建人: Lio9
 */
@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "*")
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    @Autowired
    private PokeapiDataService pokeapiDataService;

    // 存储导入任务的线程安全Map
    private final ConcurrentHashMap<String, ImportTaskStatus> importTasks = new ConcurrentHashMap<>();

    /**
     * 统一导入所有数据（从PokeAPI）
     * 异步接口，立即返回任务ID
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> importAllData() {
        Map<String, Object> result = new HashMap<>();
        String taskId = generateTaskId();

        try {
            ImportTaskStatus status = new ImportTaskStatus(taskId, "IMPORT_ALL", "pending", 0);
            importTasks.put(taskId, status);

            logger.info("启动全量数据导入任务，任务ID: {}", taskId);

            // 异步执行导入
            CompletableFuture.runAsync(() -> {
                try {
                    status.setStatus("running");
                    status.setMessage("正在清空旧数据...");
                    status.setProgress(2);
                    logger.info("任务 {} - 开始清空旧数据", taskId);

                    // 调用统一的导入方法，该方法会自动清空所有表数据
                    status.setMessage("开始导入所有数据...");
                    status.setProgress(5);
                    logger.info("任务 {} - 开始导入所有数据", taskId);

                    Map<String, Object> importResult = pokeapiDataService.importAllPokemonData();

                    status.setProgress(100);
                    status.setStatus("completed");

                    if (importResult.containsKey("success") && (Boolean) importResult.get("success")) {
                        status.setMessage(String.format("数据导入完成！宝可梦: %d, 技能: %d, 物品: %d, 特性: %d",
                            importResult.get("pokemonCount"),
                            importResult.get("moveCount"),
                            importResult.get("itemCount"),
                            importResult.get("abilityCount")));
                        status.setData(Map.of(
                            "pokemonCount", importResult.get("pokemonCount"),
                            "moveCount", importResult.get("moveCount"),
                            "itemCount", importResult.get("itemCount"),
                            "abilityCount", importResult.get("abilityCount"),
                            "startTime", status.getStartTime(),
                            "endTime", System.currentTimeMillis()
                        ));
                    } else {
                        status.setMessage("导入失败: " + importResult.get("error"));
                    }

                } catch (Exception e) {
                    logger.error("任务 {} - 导入失败: {}", taskId, e.getMessage(), e);
                    status.setStatus("failed");
                    status.setMessage("导入失败: " + e.getMessage());
                }
            });

            result.put("code", 200);
            result.put("message", "导入任务已启动，请使用taskId查询进度");
            result.put("data", Map.of(
                "taskId", taskId,
                "statusUrl", "/api/import/status/" + taskId
            ));

        } catch (Exception e) {
            logger.error("启动导入任务失败: {}", e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "启动导入任务失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 查询导入任务状态
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<Map<String, Object>> getImportStatus(@PathVariable String taskId) {
        Map<String, Object> result = new HashMap<>();

        ImportTaskStatus status = importTasks.get(taskId);
        if (status == null) {
            result.put("code", 404);
            result.put("message", "任务不存在");
            return ResponseEntity.status(404).body(result);
        }

        result.put("code", 200);
        result.put("message", "获取状态成功");
        result.put("data", status);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有导入任务列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getAllTasks() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取任务列表成功");
        result.put("data", importTasks.values());
        return ResponseEntity.ok(result);
    }

    /**
     * 清空所有数据
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> clearAllData() {
        Map<String, Object> result = new HashMap<>();
        try {
            String clearResult = pokeapiDataService.clearAllData();
            logger.info("清空所有表数据: {}", clearResult);
            result.put("code", 200);
            result.put("message", "数据清空完成");
            result.put("data", clearResult);
        } catch (Exception e) {
            logger.error("清空数据失败: {}", e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "数据清空失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 导入指定范围的宝可梦数据
     */
    @PostMapping("/pokemon/range")
    public ResponseEntity<Map<String, Object>> importPokemonRange(
            @RequestParam(defaultValue = "1") Integer startId,
            @RequestParam(defaultValue = "100") Integer count) {
        Map<String, Object> result = new HashMap<>();
        String taskId = generateTaskId();

        try {
            ImportTaskStatus status = new ImportTaskStatus(taskId, "IMPORT_POKEMON_RANGE", "pending", 0);
            importTasks.put(taskId, status);

            logger.info("启动宝可梦范围导入任务，任务ID: {}, 范围: {}-{}", taskId, startId, startId + count - 1);

            // 异步执行导入
            CompletableFuture.runAsync(() -> {
                try {
                    status.setStatus("running");
                    status.setMessage("开始导入宝可梦数据...");
                    status.setProgress(10);

                    int endId = startId + count - 1;
                    int successCount = 0;
                    int failCount = 0;

                    for (int id = startId; id <= endId; id++) {
                        try {
                            pokeapiDataService.importPokemonById(id);
                            successCount++;

                            // 更新进度
                            if (id % 10 == 0) {
                                int progress = 10 + (int) ((double) (id - startId + 1) / count * 80);
                                status.setProgress(progress);
                                status.setMessage(String.format("正在导入宝可梦... (%d/%d) 成功: %d, 失败: %d",
                                    id, endId, successCount, failCount));
                                logger.info("任务 {} - 进度: {}%, 已处理: {}/{}", taskId, progress, id, endId);
                            }
                        } catch (Exception e) {
                            failCount++;
                            logger.error("任务 {} - 导入宝可梦 {} 失败: {}", taskId, id, e.getMessage());
                        }
                    }

                    status.setStatus("completed");
                    status.setProgress(100);
                    status.setMessage(String.format("宝可梦数据导入完成！总计: %d, 成功: %d, 失败: %d",
                        count, successCount, failCount));
                    status.setData(Map.of(
                        "startId", startId,
                        "endId", endId,
                        "totalCount", count,
                        "successCount", successCount,
                        "failCount", failCount,
                        "startTime", status.getStartTime(),
                        "endTime", System.currentTimeMillis()
                    ));
                    logger.info("任务 {} - 宝可梦数据导入完成，总计: {}, 成功: {}, 失败: {}",
                        taskId, count, successCount, failCount);

                } catch (Exception e) {
                    status.setStatus("failed");
                    status.setMessage("导入失败: " + e.getMessage());
                    status.setError(e.getMessage());
                    logger.error("任务 {} - 导入失败: {}", taskId, e.getMessage(), e);
                }
            });

            result.put("code", 200);
            result.put("message", "导入任务已启动，请使用taskId查询进度");
            result.put("data", Map.of(
                "taskId", taskId,
                "statusUrl", "/api/import/status/" + taskId
            ));

        } catch (Exception e) {
            logger.error("启动导入任务失败: {}", e.getMessage(), e);
            result.put("code", 500);
            result.put("message", "启动导入任务失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "TASK-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    /**
     * 导入任务状态内部类
     */
    private static class ImportTaskStatus {
        private String taskId;
        private String taskType;
        private String status;
        private String message;
        private int progress;
        private String error;
        private Map<String, Object> data;
        private long startTime;

        public ImportTaskStatus(String taskId, String taskType, String status, int progress) {
            this.taskId = taskId;
            this.taskType = taskType;
            this.status = status;
            this.progress = progress;
            this.startTime = System.currentTimeMillis();
            this.message = "任务已创建";
        }

        public String getTaskId() { return taskId; }
        public String getTaskType() { return taskType; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public int getProgress() { return progress; }
        public String getError() { return error; }
        public Map<String, Object> getData() { return data; }
        public long getStartTime() { return startTime; }

        public void setStatus(String status) { this.status = status; }
        public void setMessage(String message) { this.message = message; }
        public void setProgress(int progress) { this.progress = progress; }
        public void setError(String error) { this.error = error; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}