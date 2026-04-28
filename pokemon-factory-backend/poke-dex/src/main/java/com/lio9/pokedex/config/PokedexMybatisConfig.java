package com.lio9.pokedex.config;



/**
 * PokedexMybatisConfig 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * pokeDex 显式装配 MyBatis 会话工厂，避免在 Spring Boot 4 + 手动裁剪 MyBatis-Plus 自动配置时丢失 mapper 基础设施。
 */
@Configuration
public class PokedexMybatisConfig {

    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.lio9.pokedex.model");
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml")
        );
        return factoryBean.getObject();
    }

    @Bean
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
