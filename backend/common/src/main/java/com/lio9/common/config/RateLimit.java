package com.lio9.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 速率限制注解
 * <p>
 * 用于限制API接口的访问频率，防止滥用和DDoS攻击
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 时间窗口（秒）
     * 默认60秒
     */
    int timeWindow() default 60;
    
    /**
     * 最大请求次数
     * 在时间窗口内允许的最大请求数
     */
    int maxRequests();
    
    /**
     * 限流键类型
     * IP: 基于客户端IP地址
     * USER: 基于用户ID
     * API: 基于API路径
     */
    RateLimitKey keyType() default RateLimitKey.IP;
    
    /**
     * 限流错误消息
     */
    String message() default "请求过于频繁，请稍后再试";
}
