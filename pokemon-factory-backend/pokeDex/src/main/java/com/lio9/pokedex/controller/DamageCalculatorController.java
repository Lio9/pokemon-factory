package com.lio9.pokedex.controller;

import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
import com.lio9.pokedex.service.DamageCalculatorService;
import com.lio9.pokedex.vo.DamageCalculationRequest;
import com.lio9.pokedex.vo.DamageResultVO;
import com.lio9.pokedex.vo.TypeEfficacyVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 伤害计算器控制器
 */
@RestController
@RequestMapping("/api/damage")
public class DamageCalculatorController {

    private static final Logger logger = LoggerFactory.getLogger(DamageCalculatorController.class);
    private final DamageCalculatorService damageCalculatorService;

    public DamageCalculatorController(DamageCalculatorService damageCalculatorService) {
        this.damageCalculatorService = damageCalculatorService;
    }

    /**
     * 计算伤害
     */
    @PostMapping("/calculate")
    public Map<String, Object> calculateDamage(@RequestBody DamageCalculationRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("计算伤害 - 攻击方形态ID: {}, 防御方形态ID: {}, 技能ID: {}", 
                request.getAttackerFormId(), request.getDefenderFormId(), request.getMoveId());
        
        try {
            DamageResultVO damageResult = damageCalculatorService.calculateDamage(request);
            long endTime = System.currentTimeMillis();
            logger.info("计算伤害成功 - 耗时: {}ms, 伤害范围: {}-{}", 
                    (endTime - startTime), damageResult.getMinDamage(), damageResult.getMaxDamage());
            return ResultResponse.buildSuccess("success", damageResult);
        } catch (IllegalArgumentException e) {
            long endTime = System.currentTimeMillis();
            logger.warn("计算伤害失败 - 参数错误: {}, 耗时: {}ms", e.getMessage(), (endTime - startTime));
            return ResultResponse.buildCustomErrorResponse(ResponseCode.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            logger.error("计算伤害失败 - 耗时: {}ms, 错误: {}", (endTime - startTime), e.getMessage());
            return ResultResponse.buildError("计算失败", e.getMessage());
        }
    }

    /**
     * 获取属性相性表
     */
    @GetMapping("/type-efficacy")
    public Map<String, Object> getTypeEfficacyMatrix() {
        long startTime = System.currentTimeMillis();
        logger.info("获取属性相性表");
        
        try {
            Map<Integer, Map<Integer, Integer>> matrix = damageCalculatorService.getTypeEfficacyMatrix();
            long endTime = System.currentTimeMillis();
            logger.info("获取属性相性表成功 - 耗时: {}ms, 属性数量: {}", (endTime - startTime), matrix.size());
            return ResultResponse.buildSuccess("success", matrix);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            logger.error("获取属性相性表失败 - 耗时: {}ms, 错误: {}", (endTime - startTime), e.getMessage());
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }

    /**
     * 获取特定攻击属性对所有防御属性的相性
     */
    @GetMapping("/type-efficacy/{damageTypeId}")
    public Map<String, Object> getTypeEfficacyByDamageType(@PathVariable Integer damageTypeId) {
        long startTime = System.currentTimeMillis();
        logger.info("获取属性相性 - 攻击属性ID: {}", damageTypeId);
        
        try {
            List<TypeEfficacyVO> efficacyList = damageCalculatorService.getTypeEfficacyByDamageType(damageTypeId);
            long endTime = System.currentTimeMillis();
            logger.info("获取属性相性成功 - 耗时: {}ms, 相性数量: {}", (endTime - startTime), efficacyList.size());
            return ResultResponse.buildSuccess("success", efficacyList);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            logger.error("获取属性相性失败 - 攻击属性ID: {}, 耗时: {}ms, 错误: {}", 
                    damageTypeId, (endTime - startTime), e.getMessage());
            return ResultResponse.buildError("获取失败", e.getMessage());
        }
    }
}
