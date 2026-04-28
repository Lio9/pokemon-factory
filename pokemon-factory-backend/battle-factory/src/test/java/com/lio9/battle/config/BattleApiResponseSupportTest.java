package com.lio9.battle.config;



/**
 * BattleApiResponseSupportTest 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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