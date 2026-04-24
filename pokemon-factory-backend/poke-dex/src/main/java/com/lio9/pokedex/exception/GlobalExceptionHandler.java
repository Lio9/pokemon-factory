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
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理所有Controller层的异常
 *
 * @author Lio9
 * @version 2.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: [{}] {}", ex.getCode(), ex.getMessage());
        return ResultResponse.buildCustomErrorResponse(
            ResponseCode.BAD_REQUEST,
            ex.getMessage(),
            null
        );
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("资源未找到: {}", ex.getMessage());
        return ResultResponse.buildCustomErrorResponse(
            ResponseCode.NOT_FOUND,
            ex.getMessage(),
            null
        );
    }

    /**
     * 处理无效参数异常
     */
    @ExceptionHandler(InvalidParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidParameterException(InvalidParameterException ex) {
        log.warn("无效参数: {}", ex.getMessage());
        return ResultResponse.buildCustomErrorResponse(
            ResponseCode.BAD_REQUEST,
            ex.getMessage(),
            null
        );
    }

    /**
     * 处理数据导入异常
     */
    @ExceptionHandler(DataImportException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleDataImportException(DataImportException ex) {
        log.error("数据导入异常: {}", ex.getMessage(), ex);
        return ResultResponse.buildCustomErrorResponse(
            ResponseCode.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            null
        );
    }

    /**
     * 处理计算异常
     */
    @ExceptionHandler(CalculationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleCalculationException(CalculationException ex) {
        log.error("计算异常: {}", ex.getMessage(), ex);
        return ResultResponse.buildCustomErrorResponse(
            ResponseCode.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            null
        );
    }

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
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("请求的资源不存在: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ResultResponse.buildCustomErrorResponse(
            ResponseCode.NOT_FOUND,
            "请求的资源不存在",
            ex.getRequestURL()
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