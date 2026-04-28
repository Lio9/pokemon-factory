package com.lio9.battle.service;



/**
 * SkillService 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端业务服务文件。
 * 核心职责：负责定义或承载模块级业务能力，对上层暴露稳定服务接口。
 * 阅读建议：建议结合控制器和实现类一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.lio9.battle.mapper.SkillMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(SkillService.class);
    private final SkillMapper skillMapper;
    private final Map<String,Integer> defaults = new HashMap<>();

    public SkillService(SkillMapper skillMapper) {
        this.skillMapper = skillMapper;
        try {
            load();
        } catch (Exception e) {
            log.warn("初始化技能默认配置失败，将使用调用方回退值。", e);
        }
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
                    if (d instanceof Number) {
                        cd = ((Number) d).intValue();
                    } else if (d != null) {
                        try {
                            cd = Integer.parseInt(String.valueOf(d));
                        } catch (NumberFormatException exception) {
                            log.warn("解析技能默认冷却失败, skillName={}, rawCooldown={}", n, d);
                        }
                    }
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
