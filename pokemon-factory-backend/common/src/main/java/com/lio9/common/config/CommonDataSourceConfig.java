package com.lio9.common.config;



/**
 * CommonDataSourceConfig 文件说明
 * 所属模块：common 公共模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * 用 common 统一创建 SQLite DataSource，避免业务模块再各自拼接数据库路径。
 */
@Configuration
public class CommonDataSourceConfig {

    @Bean
    @ConditionalOnMissingBean
    public CommonDatabasePathResolver commonDatabasePathResolver() {
        return new CommonDatabasePathResolver();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(Environment environment, CommonDatabasePathResolver pathResolver) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(environment.getProperty("spring.datasource.driver-class-name", "org.sqlite.JDBC"));
        dataSource.setJdbcUrl(pathResolver.resolveJdbcUrl(
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("SQLITE_DB_PATH")
        ));
        return dataSource;
    }
}