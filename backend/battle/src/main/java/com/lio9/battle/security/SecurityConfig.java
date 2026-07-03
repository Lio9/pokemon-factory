package com.lio9.battle.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 对战工厂安全配置 / Battle Factory Security Configuration
 *
 * <ul>
 *   <li>登录/注册/游客/图片/伤害计算/图鉴 → 匿名访问 (permitAll)</li>
 *   <li>{@code /api/user/me} → 已登录用户</li>
 *   <li>{@code /api/battle/**}（不含 guest）→ 已登录用户</li>
 *   <li>{@code /api/battle/guest/**} → 匿名访问（不经过 JWT 过滤器）</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/api/user/login", "/api/user/register").permitAll()
                .requestMatchers("/api/damage/**").permitAll()
                .requestMatchers("/api/pokedex/**").permitAll()
                .requestMatchers("/api/battle/guest/**").permitAll()
                .requestMatchers("/api/user/me").authenticated()
                .requestMatchers("/api/battle/**").authenticated()
                .anyRequest().permitAll()
            );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
