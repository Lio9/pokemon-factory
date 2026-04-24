package com.lio9.pokedex.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 使用Caffeine作为本地缓存，提供分层缓存策略
 *
 * @author Lio9
 * @version 2.0
 * @since 2024-03-31
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存名称常量
     */
    public static final String CACHE_POKEMON_LIST = "pokemonList";
    public static final String CACHE_POKEMON_DETAIL = "pokemonDetail";
    public static final String CACHE_TYPE_EFFICACY = "typeEfficacy";
    public static final String CACHE_ABILITY_LIST = "abilityList";
    public static final String CACHE_MOVE_LIST = "moveList";
    public static final String CACHE_ITEM_LIST = "itemList";
    public static final String CACHE_MOVE_DETAIL = "moveDetail";
    public static final String CACHE_DAMAGE_CALCULATION = "damageCalculation";

    /**
     * 配置缓存管理器
     * 为不同类型的数据配置不同的缓存策略
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置分层缓存策略
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(200)
                .maximumSize(5000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats());

        // 注册自定义缓存
        cacheManager.registerCustomCache(CACHE_POKEMON_LIST, Caffeine.newBuilder()
                .initialCapacity(200)
                .maximumSize(5000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build());

        cacheManager.registerCustomCache(CACHE_POKEMON_DETAIL, Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build());

        cacheManager.registerCustomCache(CACHE_TYPE_EFFICACY, Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(200)
                .expireAfterWrite(4, TimeUnit.HOURS)
                .recordStats()
                .build());

        cacheManager.registerCustomCache(CACHE_ABILITY_LIST, Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(4, TimeUnit.HOURS)
                .recordStats()
                .build());

        cacheManager.registerCustomCache(CACHE_MOVE_LIST, Caffeine.newBuilder()
                .initialCapacity(200)
                .maximumSize(2000)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .recordStats()
                .build());

        cacheManager.registerCustomCache(CACHE_MOVE_DETAIL, Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build());

        cacheManager.registerCustomCache(CACHE_ITEM_LIST, Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(4, TimeUnit.HOURS)
                .recordStats()
                .build());

        cacheManager.registerCustomCache(CACHE_DAMAGE_CALCULATION, Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(300)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats()
                .build());

        return cacheManager;
    }
}