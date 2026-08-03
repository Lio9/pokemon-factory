package com.lio9.user.controller;

import com.lio9.common.config.RateLimit;
import com.lio9.common.config.RateLimitKey;
import com.lio9.common.response.ResultResponse;
import com.lio9.user.dto.AuthRequest;
import com.lio9.user.dto.EmailVerificationRequest;
import com.lio9.user.dto.RefreshTokenRequest;
import com.lio9.user.dto.UpdatePasswordRequest;
import com.lio9.user.dto.UpdateProfileRequest;
import com.lio9.user.dto.VerifyEmailRequest;
import com.lio9.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户认证与资料管理接口。
 *
 * <p>提供注册、登录、令牌刷新、资料更新、密码修改和登出能力。
 * 所有响应统一使用 {@link ResultResponse} 构建的 {@code {code, message, data}} 结构。</p>
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 注册并立即返回登录态 */
    @PostMapping("/register")
    @RateLimit(timeWindow = 60, maxRequests = 5, keyType = RateLimitKey.IP, message = "注册过于频繁，请1分钟后再试")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultResponse.buildCreated(userService.register(request)));
    }

    /** 登录并返回 access token + refresh token */
    @PostMapping("/login")
    @RateLimit(timeWindow = 60, maxRequests = 10, keyType = RateLimitKey.IP, message = "登录尝试过于频繁，请1分钟后再试")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(ResultResponse.buildSuccess("登录成功", userService.login(request)));
    }

    /** 用 refresh token 换取新的 access token 和 refresh token */
    @PostMapping("/refresh")
    @RateLimit(timeWindow = 60, maxRequests = 30, keyType = RateLimitKey.USER, message = "令牌刷新过于频繁")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ResultResponse.buildSuccess("令牌已刷新", userService.refresh(request)));
    }

    /** 获取当前登录用户资料 */
    @GetMapping("/me")
    public ResponseEntity<?> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResultResponse.buildCustomErrorResponse(HttpStatus.UNAUTHORIZED.value(), "未登录", "unauthorized"));
        }
        return ResponseEntity.ok(ResultResponse.buildSuccess("获取成功",
                Map.of("user", userService.getCurrentUser(authentication.getName()))));
    }

    /** 更新个人资料（昵称） */
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody UpdateProfileRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResultResponse.buildCustomErrorResponse(HttpStatus.UNAUTHORIZED.value(), "未登录", "unauthorized"));
        }
        return ResponseEntity.ok(ResultResponse.buildSuccess("资料已更新",
                userService.updateProfile(authentication.getName(), request)));
    }

    /** 修改密码 */
    @PutMapping("/me/password")
    public ResponseEntity<?> updatePassword(Authentication authentication, @RequestBody UpdatePasswordRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResultResponse.buildCustomErrorResponse(HttpStatus.UNAUTHORIZED.value(), "未登录", "unauthorized"));
        }
        userService.updatePassword(authentication.getName(), request);
        return ResponseEntity.ok(ResultResponse.buildSuccess("密码已修改，请重新登录", null));
    }

    /** 登出所有设备（递增 token 版本号，使所有令牌失效） */
    @PostMapping("/me/logout-all")
    public ResponseEntity<?> logoutAll(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResultResponse.buildCustomErrorResponse(HttpStatus.UNAUTHORIZED.value(), "未登录", "unauthorized"));
        }
        userService.logoutAll(authentication.getName());
        return ResponseEntity.ok(ResultResponse.buildSuccess("已登出所有设备", null));
    }

    /** 请求邮箱验证（发送验证邮件） */
    @PostMapping("/me/email/request-verification")
    @RateLimit(timeWindow = 300, maxRequests = 3, keyType = RateLimitKey.USER, message = "验证邮件发送过于频繁，请5分钟后再试")
    public ResponseEntity<?> requestEmailVerification(Authentication authentication,
                                                       @RequestBody EmailVerificationRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResultResponse.buildCustomErrorResponse(HttpStatus.UNAUTHORIZED.value(), "未登录", "unauthorized"));
        }
        // 开发环境返回验证令牌，生产环境应只返回成功消息
        userService.requestEmailVerification(authentication.getName(), request.email());
        return ResponseEntity.ok(ResultResponse.buildSuccess(
            "验证邮件已发送，请查收邮箱并点击验证链接",
            Map.of("message", "请查收邮箱并点击验证链接")
        ));
    }

    /** 验证邮箱 */
    @PostMapping("/email/verify")
    @RateLimit(timeWindow = 60, maxRequests = 10, keyType = RateLimitKey.IP, message = "验证操作过于频繁")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest request) {
        userService.verifyEmail(request.token());
        return ResponseEntity.ok(ResultResponse.buildSuccess("邮箱验证成功", null));
    }
}
