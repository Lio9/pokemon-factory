package com.lio9.battlefactory;



/**
 * BattleFactoryApplicationTests 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：测试文件。
 * 核心职责：负责验证目标模块的边界条件、回归行为与核心输出稳定性。
 * 阅读建议：建议先看测试名称，再看构造数据与断言意图。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
