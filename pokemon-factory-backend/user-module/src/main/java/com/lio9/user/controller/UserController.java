package com.lio9.user.controller;

import com.lio9.common.response.ResponseCode;
import com.lio9.common.response.ResultResponse;
import com.lio9.user.dto.*;
import com.lio9.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultResponse.buildCreated(userService.register(request)));
    }

    /** 登录并返回 access token + refresh token */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(ResultResponse.buildSuccess("登录成功", userService.login(request)));
    }

    /** 用 refresh token 换取新的 access token 和 refresh token */
    @PostMapping("/refresh")
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
}
