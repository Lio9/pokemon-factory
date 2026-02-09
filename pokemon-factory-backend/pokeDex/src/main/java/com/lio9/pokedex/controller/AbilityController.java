package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Ability;
import com.lio9.common.service.AbilityService;
import com.lio9.common.vo.AbilityQueryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 特性控制器
 * 创建人: Lio9
 */
@RestController
@RequestMapping("/api/abilities")
@CrossOrigin(origins = "*")
public class AbilityController {
    
    @Autowired
    private AbilityService abilityService;
    
    /**
     * 分页获取特性列表
     */
    @GetMapping("/list")
    public Map<String, Object> getAbilityList(AbilityQueryVO queryVO) {
        Map<String, Object> result = new HashMap<>();
        Page<Ability> page = new Page<>(queryVO.getCurrent(), queryVO.getSize());
        Page<Ability> abilityPage = abilityService.page(page);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", abilityPage);
        return result;
    }
    
    /**
     * 获取特性详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getAbilityDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Ability ability = abilityService.getById(id);
        
        if (ability != null) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", ability);
        } else {
            result.put("code", 404);
            result.put("message", "特性不存在");
        }
        return result;
    }
    

}