package com.lio9.pokedex.controller;



import com.lio9.common.response.ResultResponse;
import com.lio9.pokedex.service.DamageCalculatorService;
import com.lio9.pokedex.vo.DamageCalculationRequest;
import com.lio9.pokedex.vo.DamageResultVO;
import com.lio9.pokedex.vo.TypeEfficacyVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 伤害计算器控制器
 */
@RestController
@RequestMapping("/api/damage")
public class DamageCalculatorController {
    private final DamageCalculatorService damageCalculatorService;

    public DamageCalculatorController(DamageCalculatorService damageCalculatorService) {
        this.damageCalculatorService = damageCalculatorService;
    }

    /**
     * 计算伤害
     */
    @PostMapping("/calculate")
    public Map<String, Object> calculateDamage(@RequestBody DamageCalculationRequest request) {
        DamageResultVO damageResult = damageCalculatorService.calculateDamage(request);
        return ResultResponse.buildSuccess("success", damageResult);
    }

    /**
     * 获取属性相性表
     */
    @GetMapping("/type-efficacy")
    public Map<String, Object> getTypeEfficacyMatrix() {
        Map<Integer, Map<Integer, Integer>> matrix = damageCalculatorService.getTypeEfficacyMatrix();
        return ResultResponse.buildSuccess("success", matrix);
    }

    /**
     * 获取特定攻击属性对所有防御属性的相性
     */
    @GetMapping("/type-efficacy/{damageTypeId}")
    public Map<String, Object> getTypeEfficacyByDamageType(@PathVariable Integer damageTypeId) {
        List<TypeEfficacyVO> efficacyList = damageCalculatorService.getTypeEfficacyByDamageType(damageTypeId);
        return ResultResponse.buildSuccess("success", efficacyList);
    }
}
