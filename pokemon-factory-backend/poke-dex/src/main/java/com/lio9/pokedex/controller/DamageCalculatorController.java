package com.lio9.pokedex.controller;



/**
 * DamageCalculatorController 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端控制器文件。
 * 核心职责：负责承接 HTTP 请求、整理参数并调用业务层返回统一响应。
 * 阅读建议：建议先看接口入口方法，再追踪到 service 层。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
