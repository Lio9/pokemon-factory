package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.AbilityMapper;
import com.lio9.common.model.Ability;
import com.lio9.common.service.AbilityService;
import com.lio9.common.vo.AbilityQueryVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 特性服务实现类
 * 创建人: Lio9
 */
@Service
public class AbilityServiceImpl extends ServiceImpl<AbilityMapper, Ability> implements AbilityService {
    
    @Override
    public Page<Ability> getAbilityPage(Page<Ability> page, AbilityQueryVO queryVO) {
        QueryWrapper<Ability> queryWrapper = new QueryWrapper<>();
        
        // 根据名称搜索
        if (StringUtils.hasText(queryVO.getName())) {
            queryWrapper.like("name", queryVO.getName())
                       .or()
                       .like("name_en", queryVO.getName())
                       .or()
                       .like("name_jp", queryVO.getName());
        }
        
        // 排序
        queryWrapper.orderByAsc("id");
        
        return this.page(page, queryWrapper);
    }
}
