package com.lio9.pokedex.service.ability;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Ability;
import com.lio9.common.vo.AbilityVO;
import java.util.List;

/**
 * 特性服务
 */
public interface AbilityService {
    
    /**
     * 分页查询特性列表
     */
    Page<Ability> getAbilityList(Integer current, Integer size, String name);
    
    /**
     * 根据ID获取特性详情
     */
    AbilityVO getAbilityDetail(Integer abilityId);
    
    /**
     * 搜索特性
     */
    List<Ability> searchAbilities(String keyword);
    
    /**
     * 统计特性数量
     */
    long count();
}