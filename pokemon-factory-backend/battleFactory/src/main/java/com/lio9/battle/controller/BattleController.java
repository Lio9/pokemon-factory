package com.lio9.battle.controller;

import com.lio9.battle.service.BattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    @Autowired
    private BattleService battleService;

    @PostMapping("/start")
    public ResponseEntity<?> startBattle(@RequestBody Map<String, Object> req) {
        Map<String, Object> result = battleService.startMatch(req);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status/{battleId}")
    public ResponseEntity<?> status(@PathVariable Long battleId) {
        Map<String, Object> status = battleService.getBattleStatus(battleId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/pool")
    public ResponseEntity<?> pool(@RequestParam(required = false) Integer rank) {
        return ResponseEntity.ok(battleService.samplePool(rank));
    }
}
