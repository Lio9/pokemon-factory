package com.lio9.common;

import com.lio9.common.config.CommonDatabaseInitializer;
import com.lio9.common.config.CommonDataSourceConfig;
import com.lio9.common.config.CommonDatabaseProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * common 模块启动入口。
 * <p>
 * 这个入口的职责不是承载业务接口，而是统一负责数据库基础设施：
 * 1. 加载共享的数据源 / MyBatis 配置；
 * 2. 在首次启动时初始化核心表结构、对战表结构和用户表结构；
 * 3. 作为整个项目的数据库引导程序，让其他业务模块只专注业务逻辑。
 * </p>
 */
@SpringBootApplication(
        scanBasePackageClasses = {
                CommonDataSourceConfig.class,
                CommonDatabaseInitializer.class,
                CommonDatabaseProperties.class
        },
        excludeName = {
                "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration",
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
        }
)
public class CommonApplication {

    /**
     * 启动 common 数据库引导程序。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CommonApplication.class);
        // 这里显式导入 common 专用配置文件，避免依赖 common 的业务模块误加载“初始化开关”。
        // 也就是说：只有真正启动 common 这个入口时，才会打开 initialize-on-startup。
        application.setDefaultProperties(Map.of(
                "spring.config.import",
                "optional:classpath:application-common.yml,optional:classpath:application-common-runner.yml"
        ));
        application.run(args);
    }
}
