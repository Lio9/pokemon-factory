package com.lio9.user.security;



/**
 * UserModuleSecurityConfig 文件说明
 * 所属模块：user-module 后端模块。
 * 文件类型：后端安全配置文件。
 * 核心职责：负责认证鉴权、过滤链或安全边界相关逻辑。
 * 阅读建议：建议重点关注请求进入系统前的校验链路。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.lio9.user.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * user-module 独立启动时的安全配置。
 * <p>
 * 只有显式开启 standalone 开关时才会装配，避免它作为 battleFactory 依赖时抢占现有安全链。
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "user.module.standalone.enabled", havingValue = "true")
public class UserModuleSecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("user-module 独立模式不使用默认用户名密码登录");
        };
    }

    @Bean
    public UserModuleJwtAuthenticationFilter userModuleJwtAuthenticationFilter(UserService userService) {
        return new UserModuleJwtAuthenticationFilter(userService);
    }

    @Bean
    public SecurityFilterChain userModuleFilterChain(HttpSecurity http,
                                                     UserModuleJwtAuthenticationFilter jwtFilter) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/api/user/login", "/api/user/register").permitAll()
                .requestMatchers("/api/user/me").authenticated()
                .anyRequest().permitAll()
        );
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
