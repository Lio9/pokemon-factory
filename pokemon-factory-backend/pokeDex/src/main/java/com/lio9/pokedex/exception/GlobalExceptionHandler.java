package com.lio9.pokedex.exception;

import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理所有Controller层的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数验证异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        
        log.warn("参数验证失败: {}", errors);
        
        return ResultResponse.buildValidationFailed(
            "参数验证失败",
            errors.toString()
        );
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        
        log.warn("参数绑定失败: {}", errors);
        
        return ResultResponse.buildValidationFailed(
            "参数绑定失败",
            errors.toString()
        );
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        
        return ResultResponse.buildCustomErrorResponse(
            ResponseCode.BAD_REQUEST,
            "非法参数",
            ex.getMessage()
        );
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleNullPointerException(NullPointerException ex) {
        log.error("空指针异常", ex);
        
        return ResultResponse.buildCustomErrorResponse(
            ResponseCode.INTERNAL_SERVER_ERROR,
            "服务器内部错误",
            "空指针异常"
        );
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception ex) {
        log.error("未捕获的异常", ex);
        
        return ResultResponse.buildCustomErrorResponse(
            ResponseCode.INTERNAL_SERVER_ERROR,
            "服务器内部错误",
            ex.getMessage()
        );
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleRuntimeException(RuntimeException ex) {
        log.error("运行时异常", ex);
        
        return ResultResponse.buildCustomErrorResponse(
            ResponseCode.INTERNAL_SERVER_ERROR,
            "服务器内部错误",
            ex.getMessage()
        );
    }
}