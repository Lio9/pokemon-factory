package com.lio9.battle.config;



import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BattleApiResponseSupportTest {

    @Test
    void fromPayload_wrapsSuccessfulPayloadIntoStandardEnvelope() {
        ResponseEntity<Map<String, Object>> response = BattleApiResponseSupport.fromPayload(Map.of("battleId", 42));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody().get("data"));
        assertEquals(200, response.getBody().get("code"));
        assertEquals(42, ((Map<?, ?>) response.getBody().get("data")).get("battleId"));
    }

    @Test
    void fromPayload_mapsLegacyErrorPayloadToHttpErrorEnvelope() {
        ResponseEntity<Map<String, Object>> response = BattleApiResponseSupport.fromPayload(Map.of(
                "error", "preview_required",
                "summary", Map.of("status", "preview")
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("code"));
        assertEquals("preview_required", response.getBody().get("error"));
        assertInstanceOf(Map.class, response.getBody().get("data"));
        assertEquals("preview", ((Map<?, ?>) ((Map<?, ?>) response.getBody().get("data")).get("summary")).get("status"));
    }
}