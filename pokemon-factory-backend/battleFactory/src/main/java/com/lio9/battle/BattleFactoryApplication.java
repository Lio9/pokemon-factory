package com.lio9.battle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * BattleFactory 应用启动类。
 * <p>
 * battleFactory 现在只负责对战工厂业务：
 * 1. 提供对战、匹配、异步任务和鉴权接口；
 * 2. 复用 common 提供的统一数据库连接和初始化结果；
 * 3. 不再自己维护数据库迁移目录与底层连接配置。
 * </p>
 */
@SpringBootApplication(excludeName = {
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
// battleFactory 只扫描自己的业务 Bean 和 user-module 的认证 Bean；
// common 中的 ServiceImpl 主要服务图鉴模块，不能再整包扫进来，否则会把不需要的 MyBatis-Plus 依赖一起拉入启动链。
@ComponentScan(basePackages = {"com.lio9.battle","com.lio9.user"})
// Mapper 仍需保留 common.mapper，因为对战计算会直接复用图鉴公共表数据。
@MapperScan({"com.lio9.battle.mapper","com.lio9.user.mapper","com.lio9.common.mapper"})
public class BattleFactoryApplication {

    /**
     * 启动对战工厂业务模块。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BattleFactoryApplication.class, args);
    }
}
