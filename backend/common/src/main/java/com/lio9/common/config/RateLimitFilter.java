package com.lio9.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 速率限制过滤器
 * <p>
 * 基于滑动窗口算法实现API访问频率限制，支持按IP、用户ID、API路径三种维度限流
 * </p>
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RateLimitFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    
    private final Map<String, long[]> rateLimitStore = new ConcurrentHashMap<>();
    private final RequestMappingHandlerMapping handlerMapping;
    
    public RateLimitFilter(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        // 包装请求，缓存 body 使 getHandler() 不会消耗 InputStream
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        try {
            HandlerExecutionChain executionChain = handlerMapping.getHandler(wrappedRequest);
            if (executionChain == null) {
                filterChain.doFilter(request, response);
                return;
            }
            Object handler = executionChain.getHandler();
            
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
                if (rateLimit == null) {
                    rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
                }
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
            filterChain.doFilter(wrappedRequest, response);
        } catch (Exception e) {
            log.error("Rate limit filter error", e);
            filterChain.doFilter(wrappedRequest, response);
        }
    }
    
    /**
     * 缓存请求 body 的包装类，解决 InputStream 只能读一次的问题。
     * getHandler() 和 @RequestBody 都能正常读取 body。
     */
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            // 读取并缓存 body 内容
            try (var is = request.getInputStream()) {
                this.cachedBody = is.readAllBytes();
            }
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            return new jakarta.servlet.ServletInputStream() {
                private final ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);

                @Override
                public int read() { return bais.read(); }

                @Override
                public boolean isFinished() { return bais.available() == 0; }

                @Override
                public boolean isReady() { return true; }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener listener) { }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
    
    private String buildLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder("rate_limit:");
        switch (rateLimit.keyType()) {
            case IP: keyBuilder.append("ip:").append(getClientIp(request)); break;
            case USER:
                // 优先从认证上下文获取用户 ID，避免客户端伪造 Header
                String remoteUser = request.getRemoteUser();
                if (remoteUser != null && !remoteUser.isEmpty()) {
                    keyBuilder.append("user:").append(remoteUser);
                } else {
                    // 回退到 IP 限流，避免 Header 伪造绕过
                    keyBuilder.append("user:ip:").append(getClientIp(request));
                }
                break;
            case API: keyBuilder.append("api:").append(request.getRequestURI()); break;
            default: keyBuilder.append("default:").append(getClientIp(request));
        }
        long windowStart = System.currentTimeMillis() / (rateLimit.timeWindow() * 1000L);
        keyBuilder.append(":").append(windowStart);
        return keyBuilder.toString();
    }
    
    private boolean checkRateLimit(String limitKey, RateLimit rateLimit) {
        long now = System.currentTimeMillis();
        long windowMillis = rateLimit.timeWindow() * 1000L;
        long windowStart = now - windowMillis;
        long[] counter = rateLimitStore.compute(limitKey, (key, value) -> {
            if (value == null || value[0] < windowStart) {
                return new long[]{now, 1};
            } else {
                value[1]++;
                return value;
            }
        });
        cleanupOldEntries(windowStart);
        return counter[1] <= rateLimit.maxRequests();
    }
    
    private void cleanupOldEntries(long windowStart) {
        if (rateLimitStore.size() < 1000) return;
        rateLimitStore.entrySet().removeIf(entry -> entry.getValue()[0] < windowStart);
    }
    
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
