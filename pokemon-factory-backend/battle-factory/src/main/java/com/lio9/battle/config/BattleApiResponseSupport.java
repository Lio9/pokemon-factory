package com.lio9.battle.config;



/**
 * BattleApiResponseSupport 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.lio9.common.response.ResultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * battleFactory 接口统一响应封装工具。
 * <p>
 * 现有 battle service 仍然以 Map 形式返回业务结果，
 * 这里负责把这些原始结果包装成统一的 code/message/data 结构，
 * 同时把旧的 error 字段映射成明确的 HTTP 状态码。
 * </p>
 */
public final class BattleApiResponseSupport {

    private BattleApiResponseSupport() {
    }

    public static ResponseEntity<Map<String, Object>> success(Object data) {
        return ResponseEntity.ok(ResultResponse.buildSuccess(data));
    }

    public static ResponseEntity<Map<String, Object>> created(Object data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResultResponse.buildCreated(data));
    }

    public static ResponseEntity<Map<String, Object>> fromPayload(Object payload) {
        if (payload instanceof Map<?, ?> rawMap && rawMap.get("error") != null) {
            return fromErrorPayload(rawMap);
        }
        return success(payload);
    }

    public static ResponseEntity<Map<String, Object>> error(String errorCode) {
        return error(statusFor(errorCode), errorCode, defaultMessage(errorCode), null);
    }

    public static ResponseEntity<Map<String, Object>> error(HttpStatus status, String errorCode, String message) {
        return error(status, errorCode, message, null);
    }

    public static ResponseEntity<Map<String, Object>> error(HttpStatus status, String errorCode, String message, Object data) {
        return ResponseEntity.status(status).body(errorBody(status, errorCode, message, data));
    }

    public static Map<String, Object> errorBody(String errorCode) {
        HttpStatus status = statusFor(errorCode);
        return errorBody(status, errorCode, defaultMessage(errorCode), null);
    }

    public static Map<String, Object> errorBody(HttpStatus status, String errorCode, String message) {
        return errorBody(status, errorCode, message, null);
    }

    public static Map<String, Object> errorBody(HttpStatus status, String errorCode, String message, Object data) {
        return ResultResponse.buildCustomErrorResponse(status.value(), message, errorCode, data);
    }

    public static HttpStatus statusFor(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (errorCode) {
            case "not_found", "team_not_found", "battle_state_missing", "run_not_active", "no_active_run" -> HttpStatus.NOT_FOUND;
            case "not_your_run" -> HttpStatus.FORBIDDEN;
            case "already_completed", "team_stale" -> HttpStatus.CONFLICT;
            case "persist_failed", "apply_failed", "replacement_confirm_failed", "forfeit_failed",
                    "preview_confirm_failed", "exchange_failed", "internal_error", "submit_failed" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    public static String defaultMessage(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return "请求处理失败。";
        }
        return switch (errorCode) {
            case "missing_fields" -> "请求缺少必要字段。";
            case "not_found" -> "未找到对应的对战记录。";
            case "preview_required" -> "请先完成队伍预览与首发选择。";
            case "replacement_required" -> "请先完成替补选择。";
            case "persist_failed" -> "保存对战数据失败。";
            case "apply_failed" -> "提交当前回合失败。";
            case "battle_state_missing" -> "当前对战缺少可用状态数据。";
            case "replacement_closed" -> "当前不是替补阶段。";
            case "replacement_confirm_failed" -> "确认替补失败。";
            case "already_completed" -> "当前对战已结束。";
            case "forfeit_failed" -> "认输失败。";
            case "preview_closed" -> "当前不是预览确认阶段。";
            case "preview_confirm_failed" -> "确认预览失败。";
            case "exchange_not_available" -> "当前不可执行交换。";
            case "team_not_found" -> "未找到可用队伍。";
            case "invalid_replaced_index" -> "替换位置无效。";
            case "team_stale" -> "队伍状态已过期，请刷新后重试。";
            case "exchange_failed" -> "交换失败。";
            case "run_not_active" -> "该工厂挑战已结束或不存在。";
            case "not_your_run" -> "无权访问该工厂挑战。";
            case "no_active_run" -> "没有进行中的工厂挑战。";
            case "replacement_count_mismatch" -> "替补数量与当前要求不一致。";
            case "invalid_replacement_choice" -> "替补选择无效。";
            case "submit_failed" -> "异步对战提交失败。";
            case "invalid_move_map_json" -> "出招数据格式无效。";
            case "invalid_move_map" -> "无法序列化当前出招数据。";
            case "serialize_failed" -> "服务器序列化对战数据失败。";
            case "internal_error" -> "服务器内部错误，请稍后重试。";
            default -> errorCode.replace('_', ' ');
        };
    }

    private static ResponseEntity<Map<String, Object>> fromErrorPayload(Map<?, ?> rawMap) {
        String errorCode = String.valueOf(rawMap.get("error"));
        HttpStatus status = statusFor(errorCode);
        String message = rawMap.get("message") instanceof String rawMessage && !rawMessage.isBlank()
                ? rawMessage
                : defaultMessage(errorCode);

        Map<String, Object> data = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> {
            String normalizedKey = String.valueOf(key);
            if (!"error".equals(normalizedKey) && !"message".equals(normalizedKey)) {
                data.put(normalizedKey, value);
            }
        });

        return error(status, errorCode, message, data.isEmpty() ? null : data);
    }
}
