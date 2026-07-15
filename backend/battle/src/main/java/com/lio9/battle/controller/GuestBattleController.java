package com.lio9.battle.controller;

import com.lio9.battle.config.BattleApiResponseSupport;
import com.lio9.battle.service.BattleService;
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

    public GuestBattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    /** 生成唯一的游客用户名 */
    static String generateGuestId() {
        long suffix = RANDOM.nextLong() & 0xFFFFFFFFFFL; // 40-bit → 最多 10 hex chars
        return GUEST_PREFIX + Long.toHexString(suffix);
    }

    /**
     * 以游客身份开始同步对战（返回完整初始状态）。
     *
     * <p>请求体与 {@code /api/battle/start} 一致，
     * 但无需 JWT。返回完整的 battleId + summary，前端可直接进入预览/战斗。</p>
     */
    @PostMapping("/start")
    public ResponseEntity<?> startSync(@RequestBody Map<String, Object> req) {
        String guestId = generateGuestId();
        req.put("username", guestId);
        return BattleApiResponseSupport.fromPayload(battleService.startMatch(req));
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
}
