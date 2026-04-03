package com.lio9.battle.service;

import com.lio9.battle.mapper.SkillMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkillService {
    private final SkillMapper skillMapper;
    private final Map<String,Integer> defaults = new HashMap<>();

    public SkillService(SkillMapper skillMapper) {
        this.skillMapper = skillMapper;
        try { load(); } catch (Exception ignored) {}
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
                    if (d instanceof Number) cd = ((Number)d).intValue();
                    else try { cd = Integer.parseInt(String.valueOf(d)); } catch (Exception ignore) {}
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
