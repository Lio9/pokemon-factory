package com.lio9.user.service;



/**
 * UserService 文件说明
 * 所属模块：user-module 后端模块。
 * 文件类型：后端业务服务文件。
 * 核心职责：负责定义或承载模块级业务能力，对上层暴露稳定服务接口。
 * 阅读建议：建议结合控制器和实现类一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.lio9.user.dto.AuthRequest;
import com.lio9.user.dto.AuthResponse;
import com.lio9.user.dto.UserProfile;
import com.lio9.user.mapper.UserMapper;
import com.lio9.user.model.UserAccount;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * 用户认证核心业务服务。
 * <p>
 * 主要职责：
 * 1. 校验注册/登录输入；
 * 2. 维护用户表数据与登录时间；
 * 3. 生成、解析 JWT；
 * 4. 统一向控制器抛出明确的业务错误。
 * </p>
 */
@Service
public class UserService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_-]{3,24}$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final long tokenExpMs;
    private Key signingKey;

    /**
     * 用户服务构造器。
     * <p>
     * token 过期时间已经从 common 统一管理的 YAML 配置体系中读取，
     * 这样 user-module 自己不再维护独立的数据库或认证基础设施配置。
     * </p>
     */
    public UserService(UserMapper userMapper,
                       @Value("${user.auth.token-expire-hours:24}") long tokenExpireHours) {
        this.userMapper = userMapper;
        // token 过期时间统一从 yml/环境读取，避免把配置继续硬编码在业务类里。
        this.tokenExpMs = tokenExpireHours * 60L * 60L * 1000L;
    }

    /**
     * 初始化 JWT 签名密钥。
     * <p>
     * 优先使用环境变量 JWT_SECRET；未配置时退回随机开发密钥。
     * </p>
     */
    @PostConstruct
    public void init() {
        // 用固定 secret 派生签名 key，避免开发时因为密钥长度不够直接启动失败。
        String secret = System.getenv("JWT_SECRET");
        if (secret != null && !secret.isBlank()) {
            signingKey = Keys.hmacShaKeyFor(sha256(secret));
        } else {
            signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    /**
     * 注册新用户并直接返回登录态。
     *
     * @param request 注册请求
     * @return 包含 token 与用户资料
     */
    public AuthResponse register(AuthRequest request) {
        // 注册链路会复用和登录同样的标准化校验，保证两条入口的用户名口径一致。
        requireRequest(request);
        String username = normalizeUsername(request.username());
        String password = normalizePassword(request.password());
        if (userMapper.findByUsername(username) != null) {
            throw new ResponseStatusException(CONFLICT, "用户名已存在");
        }

        try {
            userMapper.insertUser(username, username, passwordEncoder.encode(password));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(CONFLICT, "用户名已存在", e);
        }
        UserAccount account = userMapper.findByUsername(username);
        userMapper.touchLogin(account.getId());
        return buildAuthResponse(userMapper.findByUsername(username));
    }

    /**
     * 用户登录并签发新的 JWT。
     *
     * @param request 登录请求
     * @return 包含 token 与用户资料
     */
    public AuthResponse login(AuthRequest request) {
        // 登录前同样先做统一参数标准化，避免“注册能过、登录不过”的口径分裂。
        requireRequest(request);
        String username = normalizeUsername(request.username());
        String password = normalizePassword(request.password());
        UserAccount account = userMapper.findByUsername(username);
        if (account == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "用户名或密码错误");
        }

        userMapper.touchLogin(account.getId());
        UserAccount refreshed = userMapper.findByUsername(username);
        return buildAuthResponse(refreshed);
    }

    /**
     * 读取当前登录用户资料。
     *
     * @param username 用户名（通常来自已验证 token）
     * @return 最新用户资料
     */
    public UserProfile getCurrentUser(String username) {
        // /me 接口只认数据库里的当前状态，不直接信任 token 里缓存的展示字段。
        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "登录状态已失效");
        }
        return account.toProfile();
    }

    /**
     * 校验 token 并提取用户名。
     *
     * @param token Bearer token（不含前缀）
     * @return 校验通过返回用户名，否则返回 null
     */
    public String validateTokenAndGetUsername(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody().getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            // 这里返回 null 交给上游过滤器按“未认证”处理，避免把 token 解析异常伪装成成功结果。
            return null;
        }
    }

    /**
     * 组装统一认证响应。
     * <p>
     * token 中只放最小必要字段，真正展示仍以后端实时查询的用户资料为准，
     * 这样后续扩展用户资料时不会被旧 token 长时间缓存拖住。
     * </p>
     */
    private AuthResponse buildAuthResponse(UserAccount account) {
        if (account == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "用户不存在");
        }

        long now = System.currentTimeMillis();
        Date expiration = new Date(now + tokenExpMs);
        // token 里保留最小可用资料，前端刷新时仍以 /me 返回为准。
        String token = Jwts.builder()
                .setSubject(account.getUsername())
                .claim("uid", account.getId())
                .claim("displayName", account.getDisplayName())
                .setIssuedAt(new Date(now))
                .setExpiration(expiration)
                .signWith(signingKey)
                .compact();

        return new AuthResponse(token, account.toProfile());
    }

    /**
     * 规范化并校验用户名。
     *
     * @param username 原始用户名
     * @return trim 后且满足格式约束的用户名
     */
    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "用户名不能为空");
        }

        String normalized = username.trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "用户名需为 3-24 位字母、数字、中文、下划线或中划线");
        }
        return normalized;
    }

    /**
     * 校验密码基础约束。
     *
     * @param password 原始密码
     * @return 原样返回（仅做合法性检查）
     */
    private String normalizePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "密码不能为空");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "密码长度不能少于 6 位");
        }
        return password;
    }

    /**
     * 保护认证入口，禁止空请求体。
     */
    private void requireRequest(AuthRequest request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "请求体不能为空");
        }
    }

    /**
     * 使用 SHA-256 把环境变量里的任意长度密钥压成固定长度字节数组，
     * 以满足 HS256 对签名 key 长度的要求。
     */
    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法初始化 JWT 签名算法", e);
        }
    }
}
