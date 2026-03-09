package com.lio9.pokedex.controller;

import com.lio9.common.service.DamageCalculatorService;
import com.lio9.common.vo.DamageCalculationRequest;
import com.lio9.common.vo.DamageResultVO;
import com.lio9.common.vo.TypeEfficacyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 伤害计算器控制器
 */
@RestController
@RequestMapping("/api/damage")
@CrossOrigin(origins = "*")
public class DamageCalculatorController {

    @Autowired
    private DamageCalculatorService damageCalculatorService;

    /**
     * 计算伤害
     */
    @PostMapping("/calculate")
    public Map<String, Object> calculateDamage(@RequestBody DamageCalculationRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            DamageResultVO damageResult = damageCalculatorService.calculateDamage(request);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", damageResult);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "计算失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取属性相性表
     */
    @GetMapping("/type-efficacy")
    public Map<String, Object> getTypeEfficacyMatrix() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<Integer, Map<Integer, Integer>> matrix = damageCalculatorService.getTypeEfficacyMatrix();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", matrix);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取特定攻击属性对所有防御属性的相性
     */
    @GetMapping("/type-efficacy/{damageTypeId}")
    public Map<String, Object> getTypeEfficacyByDamageType(@PathVariable Integer damageTypeId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<TypeEfficacyVO> efficacyList = damageCalculatorService.getTypeEfficacyByDamageType(damageTypeId);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", efficacyList);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
}
