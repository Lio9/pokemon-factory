package com.lio9.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 速率限制过滤器
 * <p>
 * 基于滑动窗口算法实现API访问频率限制，支持按IP、用户ID、API路径三种维度限流
 * </p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    
    /**
     * 存储每个键的请求时间戳列表
     * Key: 限流键（IP/用户ID/API路径）
     * Value: 请求时间戳数组 [窗口起始时间, 请求计数]
     */
    private final Map<String, long[]> rateLimitStore = new ConcurrentHashMap<>();
    
    private final RequestMappingHandlerMapping handlerMapping;
    
    public RateLimitFilter(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 获取处理器方法
            Object handler = handlerMapping.getHandler(request).getHandler();
            
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                
                // 检查方法级别的注解
                RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
                
                // 如果方法没有注解，检查类级别
                if (rateLimit == null) {
                    rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
                }
                
                // 如果有限流注解，执行限流检查
                if (rateLimit != null) {
                    String limitKey = buildLimitKey(request, rateLimit);
                    
                    if (!checkRateLimit(limitKey, rateLimit)) {
                        log.warn("Rate limit exceeded for key: {}, endpoint: {}", 
                                limitKey, request.getRequestURI());
                        
                        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(
                            "{\"code\":429,\"message\":\"" + rateLimit.message() + "\",\"data\":null}"
                        );
                        return;
                    }
                }
            }
            
            // 通过限流检查，继续过滤链
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Rate limit filter error", e);
            // 发生错误时放行，避免影响正常业务
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * 构建限流键
     */
    private String buildLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder("rate_limit:");
        
        switch (rateLimit.keyType()) {
            case IP:
                String clientIp = getClientIp(request);
                keyBuilder.append("ip:").append(clientIp);
                break;
            case USER:
                String userId = request.getHeader("X-User-Id");
                if (userId == null || userId.isEmpty()) {
                    userId = "anonymous";
                }
                keyBuilder.append("user:").append(userId);
                break;
            case API:
                keyBuilder.append("api:").append(request.getRequestURI());
                break;
            default:
                keyBuilder.append("default:").append(getClientIp(request));
        }
        
        // 添加时间窗口标识，不同窗口独立计数
        long windowStart = System.currentTimeMillis() / (rateLimit.timeWindow() * 1000L);
        keyBuilder.append(":").append(windowStart);
        
        return keyBuilder.toString();
    }
    
    /**
     * 检查是否超过速率限制
     * 
     * @param limitKey 限流键
     * @param rateLimit 限流配置
     * @return true-允许请求，false-拒绝请求
     */
    private boolean checkRateLimit(String limitKey, RateLimit rateLimit) {
        long now = System.currentTimeMillis();
        long windowMillis = rateLimit.timeWindow() * 1000L;
        long windowStart = now - windowMillis;
        
        // 使用compute原子操作，保证线程安全
        long[] counter = rateLimitStore.compute(limitKey, (key, value) -> {
            if (value == null || value[0] < windowStart) {
                // 新窗口或窗口已过期，重置计数器
                return new long[]{now, 1};
            } else {
                // 同一窗口内，增加计数
                value[1]++;
                return value;
            }
        });
        
        // 清理过期数据（避免内存泄漏）
        cleanupOldEntries(windowStart);
        
        return counter[1] <= rateLimit.maxRequests();
    }
    
    /**
     * 清理过期的限流记录
     */
    private void cleanupOldEntries(long windowStart) {
        // 每100次清理一次，避免频繁遍历
        if (rateLimitStore.size() < 1000) {
            return;
        }
        
        rateLimitStore.entrySet().removeIf(entry -> entry.getValue()[0] < windowStart);
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
