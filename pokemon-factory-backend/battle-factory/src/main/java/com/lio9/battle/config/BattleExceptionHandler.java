package com.lio9.battle.config;



/**
 * BattleExceptionHandler 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * battleFactory 全局异常处理，统一错误响应格式。
 */
@RestControllerAdvice(basePackages = "com.lio9.battle")
public class BattleExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BattleExceptionHandler.class);

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
