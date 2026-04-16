package com.lio9.pokedex.controller;

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
