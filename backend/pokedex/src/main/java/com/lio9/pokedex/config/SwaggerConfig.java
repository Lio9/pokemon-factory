package com.lio9.pokedex.config;



import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 配置类
 * 提供 API 文档和交互式测试界面
 *
 * @author Lio9
 * @version 2.0
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        var errorResponseSchema = new ObjectSchema()
                .description("错误响应")
                .addProperty("code", new StringSchema().description("错误码"))
                .addProperty("message", new StringSchema().description("错误消息"))
                .addProperty("data", new ObjectSchema().description("错误详情"));

        return new OpenAPI()
                .info(new Info()
                        .title("宝可梦图鉴 API")
                        .version("2.0.0")
                        .description("""
                                Pokemon Factory API 文档
                                
                                提供完整的宝可梦数据查询和伤害计算功能：
                                - 宝可梦信息查询（列表、详情、形态、技能、特性等）
                                - 技能信息查询（列表、详情、属性效果等）
                                - 物品信息查询（列表、详情等）
                                - 特性信息查询（列表、详情、效果等）
                                - 伤害计算器（考虑属性克制、特性、道具等）
                                - 数据导入和管理
                                
                                **技术栈**: Spring Boot 4.x + MyBatis-Plus + SQLite + Caffeine Cache
                                """)
                        .contact(new Contact()
                                .name("Lio9")
                                .email("lio9@example.com")
                                .url("https://github.com/Lio9"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("本地开发环境"),
                        new Server().url("https://your-production-domain.com").description("生产环境")
                ))
                .components(new Components()
                        .addSchemas("ErrorResponse", errorResponseSchema));
    }

    /**
     * 宝可梦相关 API 分组
     */
    @Bean
    public GroupedOpenApi pokemonApi() {
        return GroupedOpenApi.builder()
                .group("宝可梦管理")
                .pathsToMatch("/api/pokemon/**", "/api/evolution/**")
                .build();
    }

    /**
     * 技能相关 API 分组
     */
    @Bean
    public GroupedOpenApi moveApi() {
        return GroupedOpenApi.builder()
                .group("技能管理")
                .pathsToMatch("/api/move/**")
                .build();
    }

    /**
     * 物品相关 API 分组
     */
    @Bean
    public GroupedOpenApi itemApi() {
        return GroupedOpenApi.builder()
                .group("物品管理")
                .pathsToMatch("/api/item/**")
                .build();
    }

    /**
     * 特性相关 API 分组
     */
    @Bean
    public GroupedOpenApi abilityApi() {
        return GroupedOpenApi.builder()
                .group("特性管理")
                .pathsToMatch("/api/ability/**")
                .build();
    }

    /**
     * 伤害计算 API 分组
     */
    @Bean
    public GroupedOpenApi damageCalculatorApi() {
        return GroupedOpenApi.builder()
                .group("伤害计算")
                .pathsToMatch("/api/calculate/**")
                .build();
    }

    /**
     * 数据导入 API 分组
     */
    @Bean
    public GroupedOpenApi importApi() {
        return GroupedOpenApi.builder()
                .group("数据导入")
                .pathsToMatch("/api/import/**")
                .build();
    }

    /**
     * 图鉴相关 API 分组
     */
    @Bean
    public GroupedOpenApi pokedexApi() {
        return GroupedOpenApi.builder()
                .group("图鉴管理")
                .pathsToMatch("/api/pokedex/**")
                .build();
    }
}