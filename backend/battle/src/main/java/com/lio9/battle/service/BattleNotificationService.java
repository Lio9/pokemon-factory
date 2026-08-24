package com.lio9.battle.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 对战状态推送服务：通过 WebSocket 向前端推送实时更新。
 * 替代前端 2 秒轮询，降低服务器负载，提升响应速度。
 */
@Service
public class BattleNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public BattleNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void pushBattleUpdate(Long battleId, Map<String, Object> summary) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "battle_update");
        payload.put("battleId", battleId);
        payload.put("summary", summary);
        messagingTemplate.convertAndSend("/topic/battle/" + battleId, (Object) payload);
    }

    public void pushBattleComplete(Long battleId, String winner) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "battle_complete");
        payload.put("battleId", battleId);
        payload.put("winner", winner);
        messagingTemplate.convertAndSend("/topic/battle/" + battleId, (Object) payload);
    }
}
