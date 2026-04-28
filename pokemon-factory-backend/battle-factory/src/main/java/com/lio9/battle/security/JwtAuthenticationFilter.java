package com.lio9.battle.security;



/**
 * JwtAuthenticationFilter 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端安全配置文件。
 * 核心职责：负责认证鉴权、过滤链或安全边界相关逻辑。
 * 阅读建议：建议重点关注请求进入系统前的校验链路。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.lio9.user.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器。
 * <p>
 * 该过滤器负责从 Authorization 头中提取 Bearer Token，
 * 校验成功后把用户名写入 Spring Security 上下文，供 battleFactory 和 user-module 复用。
 * </p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserService userService;

    /**
     * 注入用户服务，用于复用 user-module 的 token 校验能力。
     */
    public JwtAuthenticationFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    /**
     * 对每个请求执行一次 JWT 解析。
     * <p>
     * 如果 token 无效，这里不会主动抛错，而是保持未认证状态继续向后传递，
     * 最终由 Spring Security 根据接口访问规则决定是否拒绝访问。
     * </p>
     */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            String username = userService.validateTokenAndGetUsername(token);
            if (username != null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
