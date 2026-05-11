package com.lio9.battle.controller;

import com.lio9.battle.config.BattleApiResponseSupport;
import com.lio9.battle.mapper.PlayerMapper;
import com.lio9.battle.service.BattleExecutor;
import com.lio9.battle.service.BattleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;

/**
 * 游客（匿名玩家）对战控制器。
 *
 * <p>游客无需注册/登录即可直接开始对战。
 * 每次对战分配一个唯一的游客标识 "游客_XXXXXX"，
 * 前端将其保存在 localStorage 中，后续请求携带该标识。</p>
 */
@RestController
@RequestMapping("/api/battle/guest")
public class GuestBattleController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String GUEST_PREFIX = "游客_";

    private final BattleService battleService;
    private final BattleExecutor battleExecutor;
    private final PlayerMapper playerMapper;

    public GuestBattleController(BattleService battleService, BattleExecutor battleExecutor,
                                  PlayerMapper playerMapper) {
        this.battleService = battleService;
        this.battleExecutor = battleExecutor;
        this.playerMapper = playerMapper;
    }

    /** 生成唯一的游客用户名 */
    static String generateGuestId() {
        long suffix = RANDOM.nextLong() & 0xFFFFFFFFFFL; // 40-bit → 最多 10 hex chars
        return GUEST_PREFIX + Long.toHexString(suffix);
    }

    /**
     * 以游客身份开始异步对战。
     *
     * <p>请求体与 {@code /api/battle/start-async} 一致，
     * 但无需 JWT。返回 {@code {battleId, guestId}}，前端需保存 {@code guestId} 用于后续请求。</p>
     */
    @PostMapping("/start")
    public ResponseEntity<?> startAsync(@RequestBody Map<String, Object> req) {
        String guestId = generateGuestId();
        playerMapper.insertIgnore(guestId);
        Integer playerId = playerMapper.findIdByUsername(guestId);
        if (playerId == null) {
            return BattleApiResponseSupport.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "submit_failed", "无法创建游客账号");
        }
        String teamJson = req.get("teamJson") instanceof String ? req.get("teamJson").toString() : null;
        String moveJson = toMoveJson(req.get("playerMoveMap"));
        Integer battleId = battleExecutor.submitAsyncBattle(playerId, teamJson, moveJson);
        if (battleId == null) {
            return BattleApiResponseSupport.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "submit_failed", "异步对战提交失败");
        }
        return BattleApiResponseSupport.success(Map.of("battleId", battleId, "guestId", guestId));
    }

    /** 查询对战状态 */
    @GetMapping("/status/{battleId}")
    public ResponseEntity<?> status(@PathVariable Long battleId) {
        return BattleApiResponseSupport.fromPayload(battleService.getBattleStatus(battleId));
    }

    /** 确认首发 */
    @PostMapping("/{battleId}/preview")
    public ResponseEntity<?> confirmPreview(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return BattleApiResponseSupport.fromPayload(battleService.confirmTeamPreview(battleId, req));
    }

    /** 确认替补 */
    @PostMapping("/{battleId}/replacement")
    public ResponseEntity<?> confirmReplacement(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        return BattleApiResponseSupport.fromPayload(battleService.confirmReplacement(battleId, req));
    }

    /** 提交出招 */
    @PostMapping("/{battleId}/move")
    public ResponseEntity<?> move(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        Map<String, String> moveMap = normalizeMoveMap(req);
        return BattleApiResponseSupport.fromPayload(battleService.applyMove(battleId, moveMap));
    }

    /** 认输 */
    @PostMapping("/{battleId}/forfeit")
    public ResponseEntity<?> forfeit(@PathVariable Long battleId, @RequestBody Map<String, Object> req) {
        String guestId = req.containsKey("guestId") ? String.valueOf(req.get("guestId")) : "guest";
        return BattleApiResponseSupport.fromPayload(battleService.forfeitBattle(battleId, guestId));
    }

    // ── 内部 ──────────────────────────────────────────────────────────────

    private Map<String, String> normalizeMoveMap(Map<String, Object> req) {
        if (req == null) return Map.of();
        Object moveMap = req.get("playerMoveMap");
        if (moveMap != null) return battleService.parsePlayerMoveMap(moveMap);
        if (req.get("move") != null) return Map.of("__active", String.valueOf(req.get("move")));
        return Map.of();
    }

    private String toMoveJson(Object rawMoveMap) {
        try {
            return rawMoveMap == null ? null : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(rawMoveMap);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid_move_map", e);
        }
    }
}
