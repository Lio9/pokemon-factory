package com.lio9.pokedex;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * PokeDex应用启动类
 * Spring Boot应用入口，配置多模块扫描和MyBatis映射
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.lio9.pokedex", "com.lio9.common"})
@MapperScan("com.lio9.common.mapper")
public class PokeDexApplication {

    /**
     * 应用启动方法
     * 配置允许Bean定义覆盖，启动Spring Boot应用
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        SpringApplication.run(PokeDexApplication.class, args);
    }

}