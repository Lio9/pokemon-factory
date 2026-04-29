package com.lio9.battle.config;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为 battleFactory 提供统一的 JSON 序列化器，避免运行时依赖自动配置缺失导致核心服务无法注入。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}