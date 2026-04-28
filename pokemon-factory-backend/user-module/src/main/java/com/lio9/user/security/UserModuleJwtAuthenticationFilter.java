package com.lio9.user.security;



/**
 * UserModuleJwtAuthenticationFilter 文件说明
 * 所属模块：user-module 后端模块。
 * 文件类型：后端安全配置文件。
 * 核心职责：负责认证鉴权、过滤链或安全边界相关逻辑。
 * 阅读建议：建议重点关注请求进入系统前的校验链路。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.lio9.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * user-module 独立运行时使用的 JWT 过滤器。
 * <p>
 * battleFactory 已经有自己的过滤链，这里只负责补齐 user-module 单独启动时的认证能力。
 * </p>
 */
public class UserModuleJwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserService userService;

    public UserModuleJwtAuthenticationFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String username = userService.validateTokenAndGetUsername(authorization.substring(7));
            if (username != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
