package com.lio9.battle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.lio9.battle","com.lio9.user","com.lio9.common"})
@MapperScan({"com.lio9.battle.mapper","com.lio9.user.mapper","com.lio9.common.mapper"})
public class BattleFactoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(BattleFactoryApplication.class, args);
    }
}
