package com.lio9.pokedex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * PokeDex 独立服务入口
 *
 * 端口：8082
 * 职责：宝可梦图鉴查询、伤害计算
 *
 * 注意：@MapperScan 由主启动类（battle）统一管理。
 * 独立启动时使用 PokedexMybatisConfig 手动配置 MyBatis。
 */
@SpringBootApplication(excludeName = {
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@ComponentScan(basePackages = {"com.lio9.pokedex", "com.lio9.common"})
@ConfigurationPropertiesScan(basePackages = {"com.lio9.pokedex.config", "com.lio9.common.config"})
public class PokeDexApplication {

    public static void main(String[] args) {
        SpringApplication.run(PokeDexApplication.class, args);
    }
}
