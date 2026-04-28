package com.lio9.pokedex;



/**
 * PokeDexApplication 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：模块启动入口文件。
 * 核心职责：负责 Spring Boot 应用启动与基础自动装配入口定义。
 * 阅读建议：建议把它当作模块运行的总入口理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * PokeDex 应用启动类。
 * <p>
 * 当前 pokeDex 只保留图鉴业务相关能力：
 * 1. 暴露图鉴查询、导入、缓存等业务接口；
 * 2. 复用 common 模块下沉出来的数据源 / MyBatis / 数据库初始化配置；
 * 3. 不再自己维护数据库脚本和迁移入口。
 * </p>
 */
@SpringBootApplication(excludeName = {
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
// pokeDex 仍需要扫描 common 中沉淀下来的图鉴公共 Service/配置，
// 因为图鉴业务本身大量复用了 common 里的公共查询能力。
@ComponentScan(basePackages = {"com.lio9.pokedex", "com.lio9.common"})
@ConfigurationPropertiesScan(basePackages = {"com.lio9.pokedex.config", "com.lio9.common.config"})
// 图鉴查询 mapper 已全部收口到 pokeDex 包下，common 仅保留公共配置与模型。
@MapperScan("com.lio9.pokedex.mapper")
public class PokeDexApplication {

    /**
     * 启动图鉴业务模块。
     * <p>
     * 这里保留 Bean 覆盖能力，是为了兼容 common 中提供的共享 Bean
     * 与当前模块按需扩展的业务 Bean 共存。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        SpringApplication.run(PokeDexApplication.class, args);
    }

}
