package com.lio9.pokedex.config;



/**
 * PokeDexAssetProperties 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pokemon-factory.assets")
public record PokeDexAssetProperties(String imageBaseUrl) {
}