package com.lio9.battlefactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

@SpringBootTest(
    classes = {
        com.lio9.battle.BattleFactoryApplication.class,
        BattleFactoryApplicationTests.TestBeans.class
    },
    properties = {
        "spring.autoconfigure.exclude=com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
    }
)
class BattleFactoryApplicationTests {

    @TestConfiguration
    static class TestBeans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void contextLoads() {
    }

}
