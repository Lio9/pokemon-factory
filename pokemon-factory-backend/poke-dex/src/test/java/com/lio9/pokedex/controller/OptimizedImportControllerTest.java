package com.lio9.pokedex.controller;



/**
 * OptimizedImportControllerTest 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端控制器文件。
 * 核心职责：负责承接 HTTP 请求、整理参数并调用业务层返回统一响应。
 * 阅读建议：建议先看接口入口方法，再追踪到 service 层。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.lio9.pokedex.service.PokeapiDataService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OptimizedImportControllerTest {

    @Test
    void getPerformanceStats_usesServiceInterfaceWithoutImplementationCast() {
        PokeapiDataService service = new PokeapiDataService() {
            @Override
            public void importPokemonById(int id) {
            }

            @Override
            public Map<String, Object> importAllPokemonDataOptimized() {
                return Map.of("success", true);
            }

            @Override
            public CompletableFuture<Map<String, Object>> importAllPokemonDataAsync() {
                return CompletableFuture.completedFuture(Map.of("success", true));
            }

            @Override
            public Map<String, Object> getImportProgressStatus() {
                return Map.of("pokemonCount", 42);
            }

            @Override
            public Map<String, Object> getPerformanceStats() {
                return Map.of("totalRequests", 12L, "successfulRequests", 9L);
            }
        };

        OptimizedImportController controller = new OptimizedImportController(service);

        ResponseEntity<Map<String, Object>> response = controller.getPerformanceStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().get("code"));
        assertEquals("获取性能统计成功", response.getBody().get("message"));
        assertEquals(12L, ((Map<?, ?>) response.getBody().get("data")).get("totalRequests"));
    }
}
