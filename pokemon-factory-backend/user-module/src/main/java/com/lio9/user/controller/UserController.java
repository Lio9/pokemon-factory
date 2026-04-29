package com.lio9.user.controller;



import com.lio9.common.response.ResponseCode;
import com.lio9.common.response.ResultResponse;
import com.lio9.user.dto.AuthRequest;
import com.lio9.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户认证接口入口。
 * <p>
 * 这里聚合用户模块对外暴露的最小闭环能力：
 * 注册、登录、读取当前登录用户。
 * battleFactory 的鉴权过滤器会复用 user-module 生成的 JWT，
 * 因此这里的接口同时也是整个系统登录态的基础入口。
 * </p>
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    /**
     * 通过构造器注入用户业务服务，保持控制器只负责 HTTP 协议层转换。
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 注册并立即返回登录态。
     * <p>
     * 当前产品体验采用“注册成功即视为已登录”，
     * 因此前端拿到返回值后可以直接建立本地会话。
     * </p>
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultResponse.buildSuccessResponse(ResponseCode.CREATED, "注册成功", userService.register(request)));
    }

    /**
     * 登录并返回 token 与用户资料。
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(ResultResponse.buildSuccess("登录成功", userService.login(request)));
    }

    /**
     * 返回当前登录用户资料。
     * <p>
     * 前端刷新页面后会调用该接口恢复会话，
     * 所以这里既是“当前用户信息接口”，也是“本地 token 是否仍然有效”的校验点。
     * </p>
     */
    @GetMapping("/me")
    public ResponseEntity<?> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResultResponse.buildCustomErrorResponse(HttpStatus.UNAUTHORIZED.value(), "未登录", "unauthorized"));
        }
        return ResponseEntity.ok(ResultResponse.buildSuccess("获取成功", Map.of("user", userService.getCurrentUser(authentication.getName()))));
    }
}
