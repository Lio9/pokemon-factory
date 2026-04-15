package com.lio9.pokedex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * pokeDex Web 层统一配置。
 * <p>
 * 当前先把跨域策略集中到一处，避免散落在每个 controller 上：
 * 1. 默认只放行本地开发常见前端端口；
 * 2. 生产环境通过配置覆盖；
 * 3. 后续若引入网关，只需要在这里或网关层统一调整。
 * </p>
 */
@Configuration
public class PokeDexWebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public PokeDexWebConfig(
        @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173}")
        String allowedOrigins
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }
}