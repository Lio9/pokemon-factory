package com.lio9.pokedex.service.impl;



import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * 封装 PokeAPI 导入过程中的缓存、网络检查时间戳与性能统计逻辑，
 * 便于在不依赖 Spring 容器的情况下进行单元测试，并减少实现类内部状态管理噪音。
 */
final class PokeapiImportMonitor {

    private final int cacheSizeLimit;
    private final long cacheExpiryMs;
    private final LongSupplier currentTimeSupplier;

    private final Map<Integer, JsonNode> pokemonDataCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, JsonNode> speciesDataCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, Long> pokemonCacheTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, Long> speciesCacheTimestamps = new java.util.concurrent.ConcurrentHashMap<>();

    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private long totalProcessingTime;
    private final Object statsLock = new Object();

    PokeapiImportMonitor(int cacheSizeLimit, long cacheExpiryMs, LongSupplier currentTimeSupplier) {
        this.cacheSizeLimit = cacheSizeLimit;
        this.cacheExpiryMs = cacheExpiryMs;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    JsonNode getCachedPokemon(int id) {
        return getCachedData(pokemonDataCache, pokemonCacheTimestamps, id);
    }

    JsonNode getCachedSpecies(int id) {
        return getCachedData(speciesDataCache, speciesCacheTimestamps, id);
    }

    void cachePokemon(int id, JsonNode data) {
        cacheData(pokemonDataCache, pokemonCacheTimestamps, id, data);
    }

    void cacheSpecies(int id, JsonNode data) {
        cacheData(speciesDataCache, speciesCacheTimestamps, id, data);
    }

    void recordRequest(boolean success, long processingTime) {
        synchronized (statsLock) {
            totalRequests++;
            totalProcessingTime += processingTime;
            if (success) {
                successfulRequests++;
            } else {
                failedRequests++;
            }
        }
    }

    Map<String, Object> snapshotStats() {
        purgeExpiredEntries();

        Map<String, Object> stats = new HashMap<>();
        synchronized (statsLock) {
            int pokemonCacheSize = pokemonDataCache.size();
            int speciesCacheSize = speciesDataCache.size();
            stats.put("totalRequests", totalRequests);
            stats.put("successfulRequests", successfulRequests);
            stats.put("failedRequests", failedRequests);
            stats.put("successRate", totalRequests > 0 ? (double) successfulRequests / totalRequests * 100 : 0);
            stats.put("averageProcessingTime", totalRequests > 0 ? (double) totalProcessingTime / totalRequests : 0);
            // 兼容旧接口语义：cacheSize 继续表示宝可梦主缓存规模。
            stats.put("cacheSize", pokemonCacheSize);
            stats.put("pokemonCacheSize", pokemonCacheSize);
            stats.put("speciesCacheSize", speciesCacheSize);
            stats.put("totalCacheSize", pokemonCacheSize + speciesCacheSize);
        }
        return stats;
    }

    private JsonNode getCachedData(Map<Integer, JsonNode> cache, Map<Integer, Long> timestamps, int id) {
        long now = currentTimeSupplier.getAsLong();
        Long timestamp = timestamps.get(id);
        if (timestamp == null) {
            return null;
        }
        if ((now - timestamp) < cacheExpiryMs) {
            return cache.get(id);
        }
        cache.remove(id);
        timestamps.remove(id);
        return null;
    }

    private void cacheData(Map<Integer, JsonNode> cache, Map<Integer, Long> timestamps, int id, JsonNode data) {
        if (cache.size() >= cacheSizeLimit) {
            int oldestId = timestamps.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(-1);
            if (oldestId != -1) {
                cache.remove(oldestId);
                timestamps.remove(oldestId);
            }
        }
        cache.put(id, data);
        timestamps.put(id, currentTimeSupplier.getAsLong());
    }

    private void purgeExpiredEntries() {
        long now = currentTimeSupplier.getAsLong();
        purgeExpiredEntries(pokemonDataCache, pokemonCacheTimestamps, now);
        purgeExpiredEntries(speciesDataCache, speciesCacheTimestamps, now);
    }

    private void purgeExpiredEntries(Map<Integer, JsonNode> cache, Map<Integer, Long> timestamps, long now) {
        timestamps.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue()) >= cacheExpiryMs;
            if (expired) {
                cache.remove(entry.getKey());
            }
            return expired;
        });
    }
}
