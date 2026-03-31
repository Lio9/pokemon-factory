package com.lio9.pokedex.controller;

import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
import com.lio9.common.service.DamageCalculatorService;
import com.lio9.common.vo.DamageCalculationRequest;
import com.lio9.common.vo.DamageResultVO;
import com.lio9.common.vo.TypeEfficacyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
        try {
            DamageResultVO damageResult = damageCalculatorService.calculateDamage(request);
            return ResultResponse.buildSuccess("success", damageResult);
        } catch (IllegalArgumentException e) {
            return ResultResponse.buildCustomErrorResponse(ResponseCode.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            return ResultResponse.buildError("计算失败", e.getMessage());
        }
    }

    /**
     * 获取属性相性表
     */
    @GetMapping("/type-efficacy")
    public Map<String, Object> getTypeEfficacyMatrix() {
        try {
            Map<Integer, Map<Integer, Integer>> matrix = damageCalculatorService.getTypeEfficacyMatrix();
            return ResultResponse.buildSuccess("success", matrix);
        } catch (Exception e) {
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }

    /**
     * 获取特定攻击属性对所有防御属性的相性
     */
    @GetMapping("/type-efficacy/{damageTypeId}")
    public Map<String, Object> getTypeEfficacyByDamageType(@PathVariable Integer damageTypeId) {
        try {
            List<TypeEfficacyVO> efficacyList = damageCalculatorService.getTypeEfficacyByDamageType(damageTypeId);
            return ResultResponse.buildSuccess("success", efficacyList);
        } catch (Exception e) {
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }
}
