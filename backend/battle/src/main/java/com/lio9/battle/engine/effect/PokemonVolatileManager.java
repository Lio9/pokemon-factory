package com.lio9.battle.engine.effect;



import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 宝可梦临时状态管理器 (Volatiles)
 * 
 * Volatiles 是附着在单个宝可梦上的临时效果，下场时清除。
 * 例如: Protect, Confusion, Leech Seed, Substitute 等
 */
public class PokemonVolatileManager {
    private final Map<String, VolatileStatus> volatiles = new LinkedHashMap<>();

    /**
     * 添加或刷新一个 Volatile 状态
     * @param id 状态ID
     * @param duration 持续回合数（-1表示直到下场）
     * @param data 附加数据
     */
    public void addVolatile(String id, int duration, Map<String, Object> data) {
        if (volatiles.containsKey(id)) {
            // 已存在，刷新持续时间
            VolatileStatus existing = volatiles.get(id);
            if (duration > 0) {
                existing.refresh(duration);
            }
        } else {
            volatiles.put(id, new VolatileStatus(id, duration, data != null ? data : new LinkedHashMap<>()));
        }
    }

    /**
     * 添加一个简单的 Volatile 状态（无附加数据）
     */
    public void addVolatile(String id, int duration) {
        addVolatile(id, duration, null);
    }

    /**
     * 移除一个 Volatile 状态
     */
    public void removeVolatile(String id) {
        volatiles.remove(id);
    }

    /**
     * 检查是否拥有某个 Volatile 状态
     */
    public boolean hasVolatile(String id) {
        return volatiles.containsKey(id);
    }

    /**
     * 获取某个 Volatile 状态
     */
    public Optional<VolatileStatus> getVolatile(String id) {
        return Optional.ofNullable(volatiles.get(id));
    }

    /**
     * 所有 Volatile 状态递减回合
     * 返回被移除的状态列表
     */
    public java.util.List<String> tickDown() {
        java.util.List<String> expired = new java.util.ArrayList<>();
        for (Map.Entry<String, VolatileStatus> entry : volatiles.entrySet()) {
            VolatileStatus status = entry.getValue();
            if (status.decrementTurns() == 0) {
                expired.add(entry.getKey());
            }
        }
        expired.forEach(volatiles::remove);
        return expired;
    }

    /**
     * 清除所有 Volatile 状态（如下场时）
     */
    public void clearAll() {
        volatiles.clear();
    }

    /**
     * 获取所有活跃的 Volatile 状态
     */
    public Map<String, VolatileStatus> getAllVolatiles() {
        return Map.copyOf(volatiles);
    }

    /**
     * 获取活跃 Volatile 数量
     */
    public int size() {
        return volatiles.size();
    }

    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return volatiles.isEmpty();
    }

    /**
     * 转换为可序列化的 Map
     */
    public Map<String, Object> toSerializableMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, VolatileStatus> entry : volatiles.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toMap());
        }
        return result;
    }

    /**
     * 从序列化 Map 恢复
     */
    @SuppressWarnings("unchecked")
    public static PokemonVolatileManager fromSerializableMap(Map<String, Object> map) {
        PokemonVolatileManager manager = new PokemonVolatileManager();
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> volatileData) {
                    String id = entry.getKey();
                    int duration = toInt(volatileData.get("duration"), -1);
                    Map<String, Object> data = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> dataEntry : ((Map<String, Object>)volatileData).entrySet()) {
                        if (!"duration".equals(dataEntry.getKey())) {
                            data.put(dataEntry.getKey(), dataEntry.getValue());
                        }
                    }
                    manager.addVolatile(id, duration, data);
                }
            }
        }
        return manager;
    }

    private static int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }
}
