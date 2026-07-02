package com.lio9.user;

import com.lio9.common.config.CommonDataSourceConfig;
import com.lio9.common.config.CommonDatabaseInitializer;
import com.lio9.common.config.CommonDatabaseProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * user-module 独立启动入口。
 *
 * 注意：@MapperScan 由主启动类（battle）统一管理，此处不再重复声明。
 * 独立启动时通过 mybatis.mapper-locations 自动发现 mapper XML。
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
