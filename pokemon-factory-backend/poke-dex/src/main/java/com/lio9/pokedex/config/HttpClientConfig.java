package com.lio9.pokedex.config;



/**
 * HttpClientConfig 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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