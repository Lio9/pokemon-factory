package com.lio9.common.config;

/**
 * 速率限制键类型枚举
 */
public enum RateLimitKey {
    /**
     * 基于客户端IP地址限流
     */
    IP,
    
    /**
     * 基于用户ID限流
     */
    USER,
    
    /**
     * 基于API路径限流
     */
    API
}
