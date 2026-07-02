package com.lio9.user;

import com.lio9.common.config.CommonDataSourceConfig;
import com.lio9.common.config.CommonDatabaseInitializer;
import com.lio9.common.config.CommonDatabaseProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * user-module 独立启动入口（端口 8083）。
 *
 * 注意：由 BattleFactoryApplication 统一启动时不需要此入口，
 * 独立运行 user 模块时使用此入口。
 */
@SpringBootApplication(
        scanBasePackageClasses = {
                UserModuleApplication.class,
                CommonDataSourceConfig.class,
                CommonDatabaseInitializer.class,
                CommonDatabaseProperties.class
        },
        excludeName = {
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
        }
)
@MapperScan("com.lio9.user.mapper")
public class UserModuleApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(UserModuleApplication.class);
        application.setDefaultProperties(Map.of(
                "user.module.standalone.enabled", "true",
                "server.port", "8083"
        ));
        application.run(args);
    }
}
