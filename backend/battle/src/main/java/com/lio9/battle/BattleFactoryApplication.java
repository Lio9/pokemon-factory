package com.lio9.battle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * BattleFactory 独立服务入口
 *
 * 端口：8084
 * 职责：对战引擎、AI 对手、访客模式
 *
 * 注意：依赖 user 模块的 UserService 用于 JWT 校验，
 * 但不扫描 user 的 Controller，避免端口路径冲突。
 */
@SpringBootApplication(excludeName = {
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration",
    "com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration",
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration",
    "com.baomidou.mybatisplus.autoconfigure.DdlAutoConfiguration"
})
@ComponentScan(basePackages = {
    "com.lio9.battle",
    "com.lio9.pokedex.config",
    "com.lio9.pokedex.controller",
    "com.lio9.pokedex.mapper",
    "com.lio9.pokedex.model",
    "com.lio9.pokedex.service",
    "com.lio9.pokedex.util",
    "com.lio9.user.controller",
    "com.lio9.user.dto",
    "com.lio9.user.mapper",
    "com.lio9.user.model",
    "com.lio9.user.service",
    "com.lio9.common"
})
@ConfigurationPropertiesScan(basePackages = {"com.lio9.pokedex.config", "com.lio9.common.config"})
@MapperScan({"com.lio9.battle.mapper", "com.lio9.pokedex.mapper", "com.lio9.user.mapper"})
public class BattleFactoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(BattleFactoryApplication.class, args);
    }
}
