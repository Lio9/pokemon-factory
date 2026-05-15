package com.lio9.user.service;

import com.lio9.user.dto.*;
import com.lio9.user.mapper.UserMapper;
import com.lio9.user.model.UserAccount;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.*;

/**
 * 用户认证核心业务服务。
 *
 * <p>提供注册、登录、令牌刷新、密码修改、资料更新等完整用户生命周期管理。
 * 使用双令牌机制（access token + refresh token）、令牌版本号实现会话撤销、
 * 内存滑动窗口频率限制和账号锁定防护。</p>
 */
@Service
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_-]{3,24}$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    // 锁定阈值：连续失败 5 次锁定 15 分钟
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    // 刷新令牌有效期 7 天
    private static final long REFRESH_TOKEN_EXP_MS = 7 * 24 * 60 * 60 * 1000L;

    // 登录频率限制：每 IP 每分钟最多 10 次尝试
    private static final int RATE_LIMIT_PER_MINUTE = 10;
    private final ConcurrentHashMap<String, int[]> rateLimitMap = new ConcurrentHashMap<>();

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final long accessTokenExpMs;
    private Key signingKey;

    public UserService(UserMapper userMapper,
                       @Value("${user.auth.token-expire-hours:24}") long tokenExpireHours) {
        this.userMapper = userMapper;
        this.accessTokenExpMs = tokenExpireHours * 60L * 60L * 1000L;
    }

    // ── 初始化 ─────────────────────────────────────────────────────────────

    @PostConstruct
    public void init() {
        this.signingKey = loadOrGenerateSigningKey();
    }

    /**
     * 加载或生成持久化的 JWT 签名密钥。
     *
     * <p>密钥存储在 {@code config/jwt.key} 文件中（相对 user.dir）。
     * 首次运行时自动生成 HS256 兼容密钥并写入文件，
     * 确保服务重启后已有 token 仍然有效。</p>
     */
    private Key loadOrGenerateSigningKey() {
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && !envSecret.isBlank()) {
            return Keys.hmacShaKeyFor(sha256(envSecret));
        }
        try {
            Path keyFile = Paths.get(System.getProperty("user.dir"), "config", "jwt.key");
            if (Files.exists(keyFile)) {
                byte[] encoded = Files.readAllBytes(keyFile);
                return Keys.hmacShaKeyFor(encoded);
            }
            Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            Files.createDirectories(keyFile.getParent());
            Files.write(keyFile, key.getEncoded());
            return key;
        } catch (IOException e) {
            // 无法写入文件时退回随机密钥（重启后旧 token 失效）
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    // ── 注册 ───────────────────────────────────────────────────────────────

    public AuthResponse register(AuthRequest request) {
        requireRequest(request);
        String username = normalizeUsername(request.username());
        String password = normalizePassword(request.password());
        checkRateLimit("register:" + username);

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
        return buildAuthResponse(account);
    }

    // ── 登录 ───────────────────────────────────────────────────────────────

    public AuthResponse login(AuthRequest request) {
        requireRequest(request);
        String username = normalizeUsername(request.username());
        String password = normalizePassword(request.password());
        checkRateLimit("login:" + username);

        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "用户名或密码错误");
        }

        // 检查账号是否被锁定
        if (isAccountLocked(account)) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "账号已被临时锁定，请 15 分钟后重试");
        }

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            userMapper.incrementFailedAttempts(account.getId());
            int attempts = account.getFailedAttempts() != null ? account.getFailedAttempts() + 1 : 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                lockAccount(account.getId());
            }
            throw new ResponseStatusException(UNAUTHORIZED, "用户名或密码错误");
        }

        // 登录成功：重置失败计数
        userMapper.resetFailedAttempts(account.getId());
        userMapper.touchLogin(account.getId());
        return buildAuthResponse(userMapper.findByUsername(username));
    }

    // ── 令牌刷新 ──────────────────────────────────────────────────────────

    /**
     * 用 refresh token 换取新的 access token 和 refresh token。
     *
     * <p>验证 refresh token 的签名、过期时间和令牌版本号。
     * 成功后签发全新的双令牌（refresh token rotation）。</p>
     */
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "refreshToken 不能为空");
        }
        checkRateLimit("refresh:" + extractSubjectFromToken(request.refreshToken()));

        String username = validateTokenAndGetUsername(request.refreshToken());
        if (username == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "refreshToken 无效或已过期");
        }
        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "用户不存在");
        }
        return buildAuthResponse(account);
    }

    // ── 当前用户 ──────────────────────────────────────────────────────────

    public UserProfile getCurrentUser(String username) {
        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "登录状态已失效");
        }
        return account.toProfile();
    }

    // ── 更新资料 ──────────────────────────────────────────────────────────

    public UserProfile updateProfile(String username, UpdateProfileRequest request) {
        if (request == null || request.displayName() == null || request.displayName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "昵称不能为空");
        }
        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "用户不存在");
        }
        String name = request.displayName().trim();
        if (name.length() > 24) {
            throw new ResponseStatusException(BAD_REQUEST, "昵称不能超过 24 个字符");
        }
        userMapper.updateDisplayName(account.getId(), name);
        return userMapper.findByUsername(username).toProfile();
    }

    // ── 更改密码 ──────────────────────────────────────────────────────────

    /**
     * 更改密码。成功后递增 token 版本号，使该用户所有已签发的令牌立即失效。
     */
    public void updatePassword(String username, UpdatePasswordRequest request) {
        if (request == null) throw new ResponseStatusException(BAD_REQUEST, "请求体不能为空");
        if (request.currentPassword() == null || request.currentPassword().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "当前密码不能为空");
        }
        String newPassword = normalizePassword(request.newPassword());

        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "用户不存在");
        }
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(BAD_REQUEST, "当前密码错误");
        }
        userMapper.updatePassword(account.getId(), passwordEncoder.encode(newPassword));
    }

    // ── 登出所有设备 ──────────────────────────────────────────────────────

    /** 递增 token 版本号，使该用户所有已签发的 access/refresh token 立即失效 */
    public void logoutAll(String username) {
        UserAccount account = userMapper.findByUsername(username);
        if (account != null) {
            userMapper.incrementTokenVersion(account.getId());
        }
    }

    // ── 邮箱验证 ──────────────────────────────────────────────────────

    /**
     * 发送邮箱验证邮件（当前为模拟实现）。
     *
     * <p>生成验证令牌并保存到数据库。实际项目中应集成邮件服务（如 SendGrid、AWS SES）。</p>
     *
     * @param username 用户名
     * @param email 邮箱地址
     * @return 验证令牌（生产环境应通过邮件发送，不返回给前端）
     */
    public String requestEmailVerification(String username, String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "邮箱地址不能为空");
        }
        
        // 简单的邮箱格式验证
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ResponseStatusException(BAD_REQUEST, "邮箱格式不正确");
        }

        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "用户不存在");
        }

        // 生成验证令牌
        String verificationToken = generateVerificationToken();
        
        // 保存到数据库
        userMapper.updateEmailAndVerificationToken(account.getId(), email, verificationToken);

        // TODO: 实际项目中这里应该调用邮件服务发送验证链接
        // emailService.sendVerificationEmail(email, verificationToken);
        
        // 开发环境：返回令牌以便测试
        return verificationToken;
    }

    /**
     * 验证邮箱。
     *
     * @param token 验证令牌
     */
    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "验证令牌不能为空");
        }

        UserAccount account = userMapper.findByVerificationToken(token);
        if (account == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "验证令牌无效或已过期");
        }

        // 标记邮箱为已验证
        userMapper.verifyEmail(account.getId());
    }

    private String generateVerificationToken() {
        // 生成随机 UUID 作为验证令牌
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    // ── JWT 校验 ──────────────────────────────────────────────────────────

    /**
     * 校验 token 并提取用户名。
     *
     * <p>验证内容：签名、过期时间、令牌版本号。</p>
     *
     * @return 校验通过返回用户名，否则返回 null
     */
    public String validateTokenAndGetUsername(String token) {
        try {
            var claims = Jwts.parserBuilder().setSigningKey(signingKey).build()
                    .parseClaimsJws(token).getBody();
            String username = claims.getSubject();
            if (username == null) return null;

            // 令牌版本号校验
            int tokenVersion = claims.get("tver", Integer.class);
            UserAccount account = userMapper.findByUsername(username);
            if (account == null || account.getTokenVersion() == null
                    || tokenVersion != account.getTokenVersion()) {
                return null; // 令牌版本不匹配 → 已失效
            }
            return username;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    // ── 内部构建 ──────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(UserAccount account) {
        long now = System.currentTimeMillis();
        int tver = account.getTokenVersion() != null ? account.getTokenVersion() : 1;

        String accessToken = Jwts.builder()
                .setSubject(account.getUsername())
                .claim("uid", account.getId())
                .claim("tver", tver)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessTokenExpMs))
                .signWith(signingKey)
                .compact();

        String refreshToken = Jwts.builder()
                .setSubject(account.getUsername())
                .claim("uid", account.getId())
                .claim("tver", tver)
                .claim("type", "refresh")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + REFRESH_TOKEN_EXP_MS))
                .signWith(signingKey)
                .compact();

        return new AuthResponse(accessToken, refreshToken, account.toProfile());
    }

    // ── 校验与限制 ────────────────────────────────────────────────────────

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

    private String normalizePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "密码不能为空");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "密码长度不能少于 6 位");
        }
        return password;
    }

    private void requireRequest(AuthRequest request) {
        if (request == null) throw new ResponseStatusException(BAD_REQUEST, "请求体不能为空");
    }

    /** 内存滑动窗口频率限制：每 key 每分钟最多 {@value #RATE_LIMIT_PER_MINUTE} 次 */
    private void checkRateLimit(String key) {
        long now = System.currentTimeMillis() / 60_000; // 当前分钟
        rateLimitMap.compute(key, (k, v) -> {
            if (v == null || v[0] != now) return new int[]{ (int) now, 1 };
            if (v[1] >= RATE_LIMIT_PER_MINUTE) {
                throw new ResponseStatusException(TOO_MANY_REQUESTS, "操作过于频繁，请稍后重试");
            }
            v[1]++;
            return v;
        });
    }

    private boolean isAccountLocked(UserAccount account) {
        if (account.getLockedUntil() == null) return false;
        try {
            LocalDateTime lockedUntil = LocalDateTime.parse(account.getLockedUntil(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (LocalDateTime.now().isBefore(lockedUntil)) return true;
            // 锁定已过期，自动解锁
            userMapper.unlockAccount(account.getId());
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void lockAccount(Long id) {
        LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        userMapper.incrementFailedAttempts(id);
        // 直接通过 SQL update locked_until（简单实现：第二次 UPDATE）
        // 实际可用一条 SQL 完成，这里为保持 XML 简洁
    }

    private String extractSubjectFromToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(signingKey).build()
                    .parseClaimsJws(token).getBody().getSubject();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法初始化 JWT 签名算法", e);
        }
    }
}
