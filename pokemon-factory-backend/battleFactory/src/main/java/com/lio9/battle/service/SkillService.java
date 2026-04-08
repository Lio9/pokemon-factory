package com.lio9.battle.service;

import com.lio9.battle.mapper.SkillMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能附加配置服务。
 * <p>
 * 当前主要用于读取 skill_catalog 中配置的默认冷却时间，
 * 让战斗引擎在处理 Protect 等技能时可复用数据库里的可调参数。
 * </p>
 */
@Service
public class SkillService {
    private final SkillMapper skillMapper;
    private final Map<String,Integer> defaults = new HashMap<>();

    public SkillService(SkillMapper skillMapper) {
        this.skillMapper = skillMapper;
        try { load(); } catch (Exception ignored) {}
    }

    /**
     * 重新加载技能默认配置。
     */
    public synchronized void load() {
        defaults.clear();
        List<Map<String,Object>> rows = skillMapper.findAll();
        if (rows != null) {
            for (Map<String,Object> r : rows) {
                Object n = r.get("name");
                Object d = r.get("default_cooldown");
                if (n != null) {
                    int cd = 0;
                    if (d instanceof Number) cd = ((Number)d).intValue();
                    else try { cd = Integer.parseInt(String.valueOf(d)); } catch (Exception ignore) {}
                    defaults.put(n.toString().toLowerCase(), cd);
                }
            }
        }
    }

    /**
     * 查询某个技能的默认冷却；查不到时回退到调用方给出的默认值。
     */
    public int getCooldown(String skillName, int fallback) {
        if (skillName == null) return fallback;
        return defaults.getOrDefault(skillName.toLowerCase(), fallback);
    }
}
