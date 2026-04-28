package com.lio9.battle.controller;



/**
 * BattleControllerExchangeTest 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端控制器文件。
 * 核心职责：负责承接 HTTP 请求、整理参数并调用业务层返回统一响应。
 * 阅读建议：建议先看接口入口方法，再追踪到 service 层。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.PlayerMapper;
import com.lio9.battle.service.BattleExecutor;
import com.lio9.battle.service.BattleService;
import com.lio9.battle.service.FactoryRunService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleControllerExchangeTest {

    @Mock
    private BattleService battleService;
    @Mock
    private BattleExecutor battleExecutor;
    @Mock
    private PlayerMapper playerMapper;
    @Mock
    private FactoryRunService factoryRunService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BattleController controller;

    @Test
    void exchange_returnsBadRequestWhenRequiredFieldsMissing() {
        ResponseEntity<?> response = controller.exchange(Map.of(
                "battleId", 1,
                "replacedIndex", 0
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("missing_fields", body.get("error"));
    }

    @Test
    void startBattle_injectsAuthenticatedUsernameBeforeDelegating() {
        when(battleService.startMatch(anyMap())).thenReturn(Map.of("battleId", 42));

        Map<String, Object> request = new HashMap<>();
        ResponseEntity<?> response = controller.startBattle(request, new TestingAuthenticationToken("misty", null, "ROLE_USER"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(battleService).startMatch(requestCaptor.capture());
        assertEquals("misty", requestCaptor.getValue().get("username"));
    }
}
