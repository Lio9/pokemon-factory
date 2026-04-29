package com.lio9.user.config;



import com.lio9.common.response.ResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * user-module 全局异常处理，统一认证接口响应结构。
 */
@RestControllerAdvice(basePackages = "com.lio9.user")
public class UserExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UserExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String errorCode = switch (status) {
            case BAD_REQUEST -> "bad_request";
            case UNAUTHORIZED -> "unauthorized";
            case FORBIDDEN -> "forbidden";
            case CONFLICT -> "conflict";
            default -> "user_error";
        };
        return ResponseEntity.status(status)
                .body(ResultResponse.buildCustomErrorResponse(status.value(), ex.getReason(), errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("user-module 未处理异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultResponse.buildCustomErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误，请稍后重试。", "internal_error"));
    }
}