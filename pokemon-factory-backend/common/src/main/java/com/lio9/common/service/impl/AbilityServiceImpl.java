package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.AbilityMapper;
import com.lio9.common.model.Ability;
import com.lio9.common.service.AbilityService;
import com.lio9.common.vo.AbilityQueryVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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