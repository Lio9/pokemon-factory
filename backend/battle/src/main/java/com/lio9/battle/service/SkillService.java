package com.lio9.battle.service;

import com.lio9.battle.mapper.SkillMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkillService {
    private static final Logger log = LoggerFactory.getLogger(SkillService.class);
    private final SkillMapper skillMapper;
    private final DataSource dataSource;
    private final Map<String,Integer> defaults = new HashMap<>();

    public SkillService(SkillMapper skillMapper, DataSource dataSource) {
        this.skillMapper = skillMapper;
        this.dataSource = dataSource;
        try {
            ensureTable();
            load();
        } catch (Exception e) {
            log.warn("初始化技能默认配置失败，将使用调用方回退值。", e);
        }
    }

    /** 确保 skill_catalog 表存在（幂等，跨模块初始化时序的自愈方案） */
    private void ensureTable() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS skill_catalog (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, " +
                "default_cooldown INTEGER DEFAULT 0, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")");
            stmt.execute("INSERT OR IGNORE INTO skill_catalog(name, default_cooldown) VALUES ('team_shield', 2)");
            stmt.execute("INSERT OR IGNORE INTO skill_catalog(name, default_cooldown) VALUES ('protect', 2)");
        } catch (Exception e) {
            log.warn("无法创建 skill_catalog 表", e);
        }
    }

    public synchronized void load() {
        defaults.clear();
        List<Map<String,Object>> rows = skillMapper.findAll();
        if (rows != null) {
            for (Map<String,Object> r : rows) {
                Object n = r.get("name");
                Object d = r.get("default_cooldown");
                if (n != null) {
                    int cd = 0;
                    if (d instanceof Number) cd = ((Number) d).intValue();
                    else if (d != null) {
                        try { cd = Integer.parseInt(String.valueOf(d)); }
                        catch (NumberFormatException ex) { log.warn("解析冷却失败, skill={}, raw={}", n, d); }
                    }
                    defaults.put(n.toString().toLowerCase(), cd);
                }
            }
        }
    }

    public int getCooldown(String skillName, int fallback) {
        if (skillName == null) return fallback;
        return defaults.getOrDefault(skillName.toLowerCase(), fallback);
    }
}

