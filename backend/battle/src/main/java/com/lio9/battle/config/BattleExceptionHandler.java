package com.lio9.battle.config;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * battleFactory 全局异常处理，统一错误响应格式。
 */
@RestControllerAdvice(basePackages = "com.lio9.battle")
public class BattleExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BattleExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        log.warn("请求拒绝: {} {}", ex.getStatusCode(), ex.getReason());
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : "请求处理失败";
        return BattleApiResponseSupport.error(status, "request_rejected", message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("参数错误: {}", ex.getMessage());
        String errorCode = ex.getMessage() == null || ex.getMessage().isBlank() ? "bad_request" : ex.getMessage();
        return BattleApiResponseSupport.error(HttpStatus.BAD_REQUEST, errorCode, BattleApiResponseSupport.defaultMessage(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("未处理异常", ex);
        return BattleApiResponseSupport.error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "服务器内部错误，请稍后重试。");
    }
}
