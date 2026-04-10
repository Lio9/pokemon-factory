package com.lio9.user;

import com.lio9.common.config.CommonDataSourceConfig;
import com.lio9.common.config.CommonDatabaseInitializer;
import com.lio9.common.config.CommonDatabaseProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * user-module 独立启动入口。
 * <p>
 * 这个入口的目标是让用户模块既能继续作为 battleFactory 的依赖模块复用，
 * 也能在需要时单独跑成一个只承载注册、登录和会话恢复接口的服务。
 * </p>
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

    /**
     * 启动用户模块独立服务。
     * <p>
     * 这里显式打开“独立启动模式”，让只属于 user-module 自身的安全链生效；
     * 同时给一个默认端口，避免和 battleFactory / pokeDex 的常用端口撞车。
     * </p>
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(UserModuleApplication.class);
        application.setDefaultProperties(Map.of(
                "user.module.standalone.enabled", "true",
                "server.port", "8081"
        ));
        application.run(args);
    }
}
