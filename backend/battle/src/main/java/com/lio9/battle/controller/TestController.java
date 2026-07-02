package com.lio9.battle.controller;

import com.lio9.common.config.RateLimit;
import com.lio9.common.config.RateLimitKey;
import com.lio9.user.dto.AuthRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
        return Map.of("received", body);
    }

    @PostMapping("/auth")
    public Map<String, Object> testAuth(@RequestBody AuthRequest request) {
        return Map.of("username", request.username(), "password", request.password());
    }

    @PostMapping("/auth-limited")
    @RateLimit(timeWindow = 60, maxRequests = 100, keyType = RateLimitKey.IP)
    public Map<String, Object> testAuthLimited(@RequestBody AuthRequest request) {
        return Map.of("username", request.username(), "password", request.password());
    }
}
