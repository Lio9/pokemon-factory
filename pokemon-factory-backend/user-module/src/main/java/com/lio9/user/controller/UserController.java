package com.lio9.user.controller;

import com.lio9.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> req) {
        String u = req.get("username");
        String p = req.get("password");
        if (u == null || p == null) return ResponseEntity.badRequest().body(Map.of("error","missing_fields"));
        userService.register(u, p);
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> req) {
        String u = req.get("username");
        String p = req.get("password");
        if (u == null || p == null) return ResponseEntity.badRequest().body(Map.of("error","missing_fields"));
        String token = userService.loginAndCreateToken(u, p);
        if (token == null) return ResponseEntity.status(401).body(Map.of("error","invalid_credentials"));
        return ResponseEntity.ok(Map.of("token", token));
    }
}
