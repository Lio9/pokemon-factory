package com.lio9.pokedex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/** Web layer configuration 应用 Web 层统一配置
 * <p>
 * Configures CORS and Pokemon image static resource serving.
 * 配置跨域访问和宝可梦图片的静态资源映射。
 * </p>
 */
@Configuration
public class PokeDexWebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }

    /** Serve Pokemon sprite images from data/image/ at /api/pokedex/images/**
     *  将 data/image/ 目录映射为 /api/pokedex/images/** 的静态资源
     *  Supports both running from project root and from backend/ directory.
     *  支持从项目根目录或 backend/ 目录启动时的路径定位。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/pokedex/images/**")
            .addResourceLocations("file:data/image/", "file:../data/image/")
            .setCachePeriod(86400);
    }
}
