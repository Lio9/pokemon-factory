package com.lio9.pokedex.service.impl;



/**
 * AbilityServiceImpl 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端业务实现文件。
 * 核心职责：负责落地具体业务流程，通常会组合 Mapper、工具类与领域模型。
 * 阅读建议：建议关注事务边界、核心分支和依赖协作。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.Ability;
import com.lio9.pokedex.mapper.AbilityMapper;
import com.lio9.pokedex.service.AbilityService;
import com.lio9.pokedex.vo.AbilityQueryVO;
import org.springframework.stereotype.Service;

/**
 * 特性服务实现
 */
@Service
public class AbilityServiceImpl extends ServiceImpl<AbilityMapper, Ability> implements AbilityService {

    @Override
    public Page<Ability> getAbilityPage(Page<Ability> page, AbilityQueryVO queryVO) {
        QueryWrapper<Ability> wrapper = new QueryWrapper<>();
        
        if (queryVO != null) {
            if (queryVO.getName() != null && !queryVO.getName().isEmpty()) {
                wrapper.and(w -> w.like("name", queryVO.getName()).or().like("name_en", queryVO.getName()));
            }
        }
        
        wrapper.orderByAsc("id");
        return page(page, wrapper);
    }
}
