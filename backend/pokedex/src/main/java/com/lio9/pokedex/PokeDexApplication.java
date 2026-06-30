package com.lio9.pokedex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * PokeDex module entry point (library mode) PokeDex 模块入口（仅作库使用）
 * <p>
 * Note: @MapperScan is NOT used here — OneServerApplication handles all mapper scans.
 * Run OneServerApplication instead of this class directly.
 * 注意：此处不使用 @MapperScan，由 OneServerApplication 统一管理所有 mapper 扫描。
 * 请直接运行 OneServerApplication。
 * </p>
 */
@SpringBootApplication(excludeName = {
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@ComponentScan(basePackages = {"com.lio9.pokedex", "com.lio9.common"})
@ConfigurationPropertiesScan(basePackages = {"com.lio9.pokedex.config", "com.lio9.common.config"})
public class PokeDexApplication {

    public static void main(String[] args) {
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        SpringApplication.run(PokeDexApplication.class, args);
    }
}
