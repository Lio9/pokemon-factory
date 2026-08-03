package com.lio9.pokedex.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

public class MapperBeanRegistrar implements BeanDefinitionRegistryPostProcessor {
    private final String mapperBasePackage;
    public MapperBeanRegistrar(String mapperBasePackage) { this.mapperBasePackage = mapperBasePackage; }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 如果已有 @MapperScan（如 battle 模块），则跳过——由 mybatis-spring 4.0.0 的 MapperScanner 处理
        if (hasMapperScan(registry)) return;

        try {
            String pattern = "classpath*:" + mapperBasePackage.replace('.', '/') + "/*.class";
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(pattern);
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName == null || !fileName.endsWith(".class")) continue;
                String simpleName = fileName.substring(0, fileName.length() - ".class".length());
                String fullClassName = mapperBasePackage + "." + simpleName;
                Class<?> mapperClass = Class.forName(fullClassName);
                if (!mapperClass.isInterface()) continue;

                GenericBeanDefinition bd = new GenericBeanDefinition();
                bd.setBeanClass(MapperFactoryBean.class);
                bd.getConstructorArgumentValues().addIndexedArgumentValue(0, mapperClass);
                bd.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference("sqlSessionTemplate"));
                bd.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, mapperClass);
                registry.registerBeanDefinition(simpleName, bd);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("扫描 Mapper 接口失败: " + mapperBasePackage, e);
        }
    }

    /**
     * 检查是否有配置类标注了 @MapperScan（如 BattleFactoryApplication）。
     * 如果有，则由 mybatis-spring 的 MapperScanner 处理，本注册器跳过。
     */
    private boolean hasMapperScan(BeanDefinitionRegistry registry) {
        for (String beanName : registry.getBeanDefinitionNames()) {
            try {
                String className = registry.getBeanDefinition(beanName).getBeanClassName();
                if (className != null) {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(org.mybatis.spring.annotation.MapperScan.class)) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    public void postProcessBeanFactory(@SuppressWarnings("unused") org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) throws BeansException {}
}
