package com.lio9.battle.engine.effect;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Volatile 状态（临时状态）
 * 
 * 代表附着在单个宝可梦上的临时效果，如 Protect、Confusion、Leech Seed 等
 */
public class VolatileStatus {
    private final String id;
    private int duration; // -1 表示直到下场
    private final Map<String, Object> data;

    public VolatileStatus(String id, int duration, Map<String, Object> data) {
        this.id = id;
        this.duration = duration;
        this.data = data != null ? new LinkedHashMap<>(data) : new LinkedHashMap<>();
    }

    /**
     * 刷新持续时间
     */
    public void refresh(int newDuration) {
        if (newDuration > 0) {
            this.duration = newDuration;
        }
    }

    /**
     * 递减回合
     * @return 剩余回合数
     */
    public int decrementTurns() {
        if (duration > 0) {
            duration--;
        }
        return duration;
    }

    /**
     * 获取状态ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取剩余回合数
     * @return -1 表示永久（直到下场）
     */
    public int getDuration() {
        return duration;
    }

    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return duration == 0;
    }

    /**
     * 检查是否为永久状态
     */
    public boolean isPermanent() {
        return duration < 0;
    }

    /**
     * 获取附加数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) data.get(key);
    }

    /**
     * 设置附加数据
     */
    public void setData(String key, Object value) {
        data.put(key, value);
    }

    /**
     * 获取所有附加数据
     */
    public Map<String, Object> getAllData() {
        return Map.copyOf(data);
    }

    /**
     * 转换为可序列化的 Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>(data);
        result.put("duration", duration);
        return result;
    }

    @Override
    public String toString() {
        return "VolatileStatus{id='" + id + "', duration=" + duration + ", data=" + data + "}";
    }
}
