package com.lio9.pokedex.config;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.FactoryBean;

public class MapperFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> mapperInterface;
    private final SqlSessionTemplate sqlSessionTemplate;

    public MapperFactoryBean(Class<T> mapperInterface, SqlSessionTemplate sqlSessionTemplate) {
        this.mapperInterface = mapperInterface;
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    @Override
    public T getObject() { return sqlSessionTemplate.getMapper(mapperInterface); }
    @Override
    public Class<?> getObjectType() { return mapperInterface; }
    @Override
    public boolean isSingleton() { return true; }
}
