package com.lio9.battle.config;



/**
 * JacksonConfig 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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