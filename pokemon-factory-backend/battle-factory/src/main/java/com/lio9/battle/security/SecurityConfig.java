package com.lio9.battle.security;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 对战工厂安全配置。
 * <p>
 * 这里统一定义 battleFactory 当前实际使用的鉴权边界：
 * 1. 登录、注册接口允许匿名访问；
 * 2. `/api/user/me` 用于恢复会话，需要已认证；
 * 3. `/api/battle/**` 作为对战接口，全部要求携带有效 JWT。
 * </p>
 */
@Configuration
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;

    /**
     * 注入自定义 JWT 过滤器，用于把 token 解析结果放入 Spring Security 上下文。
     */
    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * 构建当前模块的安全过滤链。
     * <p>
     * 当前前后端是前后分离模式，且主要通过 JWT 做无状态认证，
     * 因此这里关闭 CSRF，并把自定义 JWT 过滤器放到用户名密码过滤器之前执行。
     * </p>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/api/user/login", "/api/user/register").permitAll()
                .requestMatchers("/api/user/me").authenticated()
                .requestMatchers("/api/battle/**").authenticated()
                .anyRequest().permitAll()
        );
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
