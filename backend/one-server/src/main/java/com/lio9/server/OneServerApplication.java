package com.lio9.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.lio9.battle", "com.lio9.pokedex",
    "com.lio9.user.controller", "com.lio9.user.service",
    "com.lio9.user.mapper", "com.lio9.user.dto", "com.lio9.user.model",
    "com.lio9.common"
})
@MapperScan({"com.lio9.battle.mapper", "com.lio9.pokedex.mapper", "com.lio9.user.mapper"})
public class OneServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OneServerApplication.class, args);
    }
}
