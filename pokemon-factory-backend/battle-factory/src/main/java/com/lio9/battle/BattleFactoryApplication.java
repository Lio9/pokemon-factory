package com.lio9.battle;



/**
 * BattleFactoryApplication 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：模块启动入口文件。
 * 核心职责：负责 Spring Boot 应用启动与基础自动装配入口定义。
 * 阅读建议：建议把它当作模块运行的总入口理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
// 对战计算当前直接复用 pokeDex 中的图鉴相性 mapper，因此这里显式扫描 pokeDex.mapper。
@MapperScan({"com.lio9.battle.mapper","com.lio9.user.mapper","com.lio9.pokedex.mapper"})
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
