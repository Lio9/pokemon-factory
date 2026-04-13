package com.lio9.battlefactory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = com.lio9.battle.BattleFactoryApplication.class,
    properties = {
        "spring.autoconfigure.exclude=com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
    }
)
class BattleFactoryApplicationTests {

    @Test
    void contextLoads() {
    }

}
