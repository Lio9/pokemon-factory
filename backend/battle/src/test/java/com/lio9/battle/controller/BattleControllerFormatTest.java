package com.lio9.battle.controller;

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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 BattleController 能正确识别 format 字段并传递给 Service。
 */
@ExtendWith(MockitoExtension.class)
class BattleControllerFormatTest {

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
    void startBattle_passesFormatToService() {
        when(battleService.startMatch(anyMap())).thenReturn(Map.of("battleId", 42));

        Map<String, Object> body = new HashMap<>();
        body.put("username", "tester");
        body.put("teamJson", "[]");
        body.put("format", "vgc63");

        TestingAuthenticationToken auth = new TestingAuthenticationToken("tester", null, "ROLE_USER");
        ResponseEntity<?> response = controller.startBattle(body, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(battleService).startMatch(captor.capture());

        Map<String, Object> captured = captor.getValue();
        assertEquals("vgc63", captured.get("format"));
        assertEquals("tester", captured.get("username"));
    }

    @Test
    void startBattle_withoutFormat_defaultsToNull() {
        when(battleService.startMatch(anyMap())).thenReturn(Map.of("battleId", 43));

        Map<String, Object> body = new HashMap<>();
        body.put("username", "tester");
        body.put("teamJson", "[]");

        TestingAuthenticationToken auth = new TestingAuthenticationToken("tester", null, "ROLE_USER");
        controller.startBattle(body, auth);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(battleService).startMatch(captor.capture());

        Map<String, Object> captured = captor.getValue();
        assertEquals("tester", captured.get("username"));
        // 不传 format 时，Service 默认使用 vgc-doubles
    }

    @Test
    void startManualBattle_passesFormat() {
        when(battleService.startMatch(anyMap())).thenReturn(Map.of("battleId", 44));

        Map<String, Object> body = new HashMap<>();
        body.put("username", "tester");
        body.put("teamJson", "[]");
        body.put("format", "vgc-singles");

        TestingAuthenticationToken auth = new TestingAuthenticationToken("tester", null, "ROLE_USER");
        ResponseEntity<?> response = controller.startBattle(body, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(battleService).startMatch(captor.capture());

        Map<String, Object> captured = captor.getValue();
        assertEquals("vgc-singles", captured.get("format"));
    }

    @Test
    void startBattle_passesGen9singlesFormat() {
        when(battleService.startMatch(anyMap())).thenReturn(Map.of("battleId", 45));

        Map<String, Object> body = new HashMap<>();
        body.put("username", "tester");
        body.put("teamJson", "[]");
        body.put("format", "gen9singles");

        TestingAuthenticationToken auth = new TestingAuthenticationToken("tester", null, "ROLE_USER");
        ResponseEntity<?> response = controller.startBattle(body, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(battleService).startMatch(captor.capture());
        assertEquals("gen9singles", captor.getValue().get("format"));
    }
}
