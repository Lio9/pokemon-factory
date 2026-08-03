package com.lio9.pokedex.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
public class PokedexMybatisConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        return interceptor;
    }

    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, MybatisPlusInterceptor mybatisPlusInterceptor) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.lio9.pokedex.model");
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml")
        );
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(config);
        factoryBean.setPlugins(mybatisPlusInterceptor);
        SqlSessionFactory sqlSessionFactory = factoryBean.getObject();
        registerMappers(sqlSessionFactory.getConfiguration());
        return sqlSessionFactory;
    }

    @Bean
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public static MapperBeanRegistrar mapperBeanRegistrar() {
        return new MapperBeanRegistrar("com.lio9.pokedex.mapper");
    }

    private void registerMappers(org.apache.ibatis.session.Configuration configuration) throws IOException, ClassNotFoundException {
        String pattern = "classpath*:com/lio9/pokedex/mapper/*.class";
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(pattern);
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            if (fileName == null || !fileName.endsWith(".class")) continue;
            String simpleName = fileName.substring(0, fileName.length() - ".class".length());
            Class<?> mapperClass = Class.forName("com.lio9.pokedex.mapper." + simpleName);
            if (mapperClass.isInterface()) {
                configuration.addMapper(mapperClass);
            }
        }
    }
}
