package com.lio9.user.service;

import com.lio9.user.mapper.UserMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private Key signingKey;

    // token validity 24h
    private final long TOKEN_EXP_MS = 24L * 60L * 60L * 1000L;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @PostConstruct
    public void init() {
        // Prefer a persistent secret from environment (JWT_SECRET). If absent, fallback to generated key.
        String secret = System.getenv("JWT_SECRET");
        if (secret != null && !secret.isBlank()) {
            signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } else {
            signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    public void register(String username, String password) {
        String hashed = passwordEncoder.encode(password);
        userMapper.insertUser(username, hashed);
    }

    public String loginAndCreateToken(String username, String password) {
        Map<String,Object> row = userMapper.findByUsername(username);
        if (row == null || row.isEmpty()) return null;
        String hash = (String) row.get("passwordHash");
        if (!passwordEncoder.matches(password, hash)) return null;
        long now = System.currentTimeMillis();
        Date exp = new Date(now + TOKEN_EXP_MS);
        return Jwts.builder().setSubject(username).setExpiration(exp).setIssuedAt(new Date(now)).signWith(signingKey).compact();
    }

    public String validateTokenAndGetUsername(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody().getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
