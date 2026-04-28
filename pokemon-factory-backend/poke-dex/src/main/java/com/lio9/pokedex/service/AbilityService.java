package com.lio9.pokedex.service;



/**
 * AbilityService 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端业务服务文件。
 * 核心职责：负责定义或承载模块级业务能力，对上层暴露稳定服务接口。
 * 阅读建议：建议结合控制器和实现类一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
