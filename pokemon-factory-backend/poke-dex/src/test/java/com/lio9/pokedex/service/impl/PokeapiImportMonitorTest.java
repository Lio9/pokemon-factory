package com.lio9.pokedex.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PokeapiImportMonitorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void pokemonAndSpeciesCachesUseIndependentExpiryTimestamps() {
        MutableTimeSupplier time = new MutableTimeSupplier();
        PokeapiImportMonitor monitor = new PokeapiImportMonitor(10, 1_000, time);
        JsonNode pokemon = MAPPER.createObjectNode().put("kind", "pokemon");
        JsonNode species = MAPPER.createObjectNode().put("kind", "species");

        monitor.cachePokemon(25, pokemon);
        time.advance(900);
        monitor.cacheSpecies(25, species);
        time.advance(600);

        assertNull(monitor.getCachedPokemon(25));
        assertSame(species, monitor.getCachedSpecies(25));
    }

    @Test
    void performanceSnapshotTracksSuccessFailureAndAverageLatency() {
        MutableTimeSupplier time = new MutableTimeSupplier();
        PokeapiImportMonitor monitor = new PokeapiImportMonitor(10, 1_000, time);

        monitor.recordRequest(true, 40);
        monitor.recordRequest(false, 80);
        monitor.cachePokemon(1, MAPPER.createObjectNode().put("id", 1));
        monitor.cacheSpecies(2, MAPPER.createObjectNode().put("id", 2));

        Map<String, Object> stats = monitor.snapshotStats();

        assertEquals(2L, stats.get("totalRequests"));
        assertEquals(1L, stats.get("successfulRequests"));
        assertEquals(1L, stats.get("failedRequests"));
        assertEquals(50.0, stats.get("successRate"));
        assertEquals(60.0, stats.get("averageProcessingTime"));
        assertEquals(1, stats.get("pokemonCacheSize"));
        assertEquals(1, stats.get("speciesCacheSize"));
        assertEquals(1, stats.get("cacheSize"));
        assertEquals(2, stats.get("totalCacheSize"));
    }

    @Test
    void expiredEntriesArePurgedBeforeReportingCacheStats() {
        MutableTimeSupplier time = new MutableTimeSupplier();
        PokeapiImportMonitor monitor = new PokeapiImportMonitor(10, 1_000, time);

        monitor.cachePokemon(25, MAPPER.createObjectNode().put("id", 25));
        monitor.cacheSpecies(26, MAPPER.createObjectNode().put("id", 26));
        time.advance(1_001);

        assertNull(monitor.getCachedPokemon(25));
        assertNull(monitor.getCachedSpecies(26));

        Map<String, Object> stats = monitor.snapshotStats();

        assertEquals(0, stats.get("pokemonCacheSize"));
        assertEquals(0, stats.get("speciesCacheSize"));
        assertEquals(0, stats.get("cacheSize"));
        assertEquals(0, stats.get("totalCacheSize"));
    }

    private static final class MutableTimeSupplier implements LongSupplier {
        private long now;

        @Override
        public long getAsLong() {
            return now;
        }

        void advance(long delta) {
            now += delta;
        }
    }
}
