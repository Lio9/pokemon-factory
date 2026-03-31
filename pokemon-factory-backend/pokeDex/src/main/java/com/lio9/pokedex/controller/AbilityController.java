package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Ability;
import com.lio9.common.service.AbilityService;
import com.lio9.common.vo.AbilityQueryVO;
import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 特性控制器
 * 提供特性数据的REST API接口，支持分页查询和条件搜索
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/abilities")
@CrossOrigin(origins = "*")
public class AbilityController {
    
    @Autowired
    private AbilityService abilityService;
    
    /**
     * 分页获取特性列表
     * 支持按名称模糊搜索和分页显示
     * 
     * @param queryVO 查询条件对象，包含分页参数和搜索条件
     * @return 分页结果，包含特性列表和分页信息
     */
    @GetMapping("/list")
    public Map<String, Object> getAbilityList(AbilityQueryVO queryVO) {
        Page<Ability> page = new Page<>(queryVO.getCurrent(), queryVO.getSize());
        Page<Ability> abilityPage = abilityService.page(page);
        
        return ResultResponse.buildPageSuccess(abilityPage);
    }
    
    /**
     * 获取特性详情
     * 根据特性ID获取特性的详细信息
     * 
     * @param id 特性ID
     * @return 特性详情信息
     */
    @GetMapping("/{id}")
    public Map<String, Object> getAbilityDetail(@PathVariable Long id) {
        Ability ability = abilityService.getById(id);
        
        if (ability != null) {
            return ResultResponse.buildSuccess("success", ability);
        } else {
            return ResultResponse.buildNotFound("特性", id);
        }
    }
}
