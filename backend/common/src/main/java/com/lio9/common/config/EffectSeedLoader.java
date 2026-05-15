package com.lio9.common.config;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

/**
 * 从 JSON 配置文件加载特性/道具效果种子并写入数据库。
 * <p>
 * 废弃旧版硬编码 Java 数组，改为 effect-seeds/*.json 外部配置，
 * 新增特性/道具效果时只需要编辑 JSON 文件，无需修改 Java 代码。
 * </p>
 */
@Component
public class EffectSeedLoader {
    private static final Logger log = LoggerFactory.getLogger(EffectSeedLoader.class);

    public void syncEffectSeeds(Connection connection) throws Exception {
        boolean originalAutoCommit = connection.getAutoCommit();
        if (originalAutoCommit) {
            connection.setAutoCommit(false);
        }
        try {
            importAbilityEffects(connection);
            importItemEffects(connection);
            if (originalAutoCommit) {
                connection.commit();
            }
        } catch (Exception e) {
            if (originalAutoCommit) {
                connection.rollback();
            }
            throw e;
        } finally {
            if (originalAutoCommit) {
                connection.setAutoCommit(true);
            }
        }
    }

    private void importAbilityEffects(Connection connection) throws Exception {
        List<Map<String, Object>> effects = loadJson("effect-seeds/ability-effects.json");
        try (PreparedStatement del = connection.prepareStatement("DELETE FROM ability_effect");
             PreparedStatement stmt = connection.prepareStatement(
                     "INSERT INTO ability_effect (ability_id, effect_type, effect_value, target, condition, description) VALUES (?, ?, ?, ?, ?, ?)")) {
            del.executeUpdate();
            for (Map<String, Object> row : effects) {
                stmt.setInt(1, toInt(row.get("id")));
                stmt.setString(2, (String) row.get("effect_type"));
                stmt.setString(3, (String) row.get("effect_value"));
                stmt.setString(4, (String) row.get("target"));
                stmt.setString(5, (String) row.get("condition"));
                stmt.setString(6, (String) row.get("description"));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        log.info("导入 ability_effect 种子数据：{} 条", effects.size());
    }

    private void importItemEffects(Connection connection) throws Exception {
        List<Map<String, Object>> effects = loadJson("effect-seeds/item-effects.json");
        try (PreparedStatement del = connection.prepareStatement("DELETE FROM item_effect");
             PreparedStatement stmt = connection.prepareStatement(
                     "INSERT INTO item_effect (item_id, effect_type, effect_value, target, condition, description) VALUES (?, ?, ?, ?, ?, ?)")) {
            del.executeUpdate();
            for (Map<String, Object> row : effects) {
                stmt.setInt(1, toInt(row.get("id")));
                stmt.setString(2, (String) row.get("effect_type"));
                stmt.setString(3, (String) row.get("effect_value"));
                stmt.setString(4, (String) row.get("target"));
                stmt.setString(5, (String) row.get("condition"));
                stmt.setString(6, (String) row.get("description"));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        log.info("导入 item_effect 种子数据：{} 条", effects.size());
    }

    private List<Map<String, Object>> loadJson(String path) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(
                new ClassPathResource(path).getInputStream(),
                new TypeReference<List<Map<String, Object>>>() {});
    }

    private int toInt(Object value) {
        if (value instanceof Integer i) return i;
        if (value instanceof Long l) return l.intValue();
        if (value instanceof String s) return Integer.parseInt(s);
        return 0;
    }
}
