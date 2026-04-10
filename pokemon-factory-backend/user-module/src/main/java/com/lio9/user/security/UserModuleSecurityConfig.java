package com.lio9.user.security;

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
                .requestMatchers("/api/user/login", "/api/user/register").permitAll()
                .requestMatchers("/api/user/me").authenticated()
                .anyRequest().permitAll()
        );
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
