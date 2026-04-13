package com.lio9.pokedex.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lio9.pokedex.model.Ability;
import com.lio9.pokedex.vo.AbilityQueryVO;

/**
 * 特性服务接口
 * 创建人: Lio9
 */
public interface AbilityService extends IService<Ability> {
    
    /**
     * 分页查询特性列表
     * @param page 分页对象
     * @param queryVO 查询参数
     * @return 分页结果
     */
    Page<Ability> getAbilityPage(Page<Ability> page, AbilityQueryVO queryVO);
}
