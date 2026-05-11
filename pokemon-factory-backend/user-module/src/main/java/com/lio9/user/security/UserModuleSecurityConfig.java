package com.lio9.user.security;

import com.lio9.user.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * user-module 安全配置。
 *
 * <p>只有 {@code user.module.standalone.enabled=true} 时生效，
 * 作为 battleFactory 依赖时不装配，避免抢占已有的安全过滤链。</p>
 */
@Configuration
@ConditionalOnProperty(name = "user.module.standalone.enabled", havingValue = "true")
public class UserModuleSecurityConfig {

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
                .requestMatchers("/api/user/login", "/api/user/register", "/api/user/refresh").permitAll()
                .requestMatchers("/api/user/me", "/api/user/me/**").authenticated()
                .anyRequest().permitAll()
        );
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
