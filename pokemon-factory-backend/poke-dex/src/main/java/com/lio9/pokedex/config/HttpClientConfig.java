package com.lio9.pokedex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP客户端配置
 * 提供RestTemplate的Bean配置，用于HTTP请求和PokeAPI调用
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
public class HttpClientConfig {
    
    /**
     * 创建RestTemplate Bean
     * 用于发送HTTP请求和调用PokeAPI接口
     * 
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}