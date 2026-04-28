package com.lio9.pokedex.service;



/**
 * DamageCalculatorService 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端业务服务文件。
 * 核心职责：负责定义或承载模块级业务能力，对上层暴露稳定服务接口。
 * 阅读建议：建议结合控制器和实现类一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.lio9.pokedex.vo.DamageCalculationRequest;
import com.lio9.pokedex.vo.DamageResultVO;
import com.lio9.pokedex.vo.TypeEfficacyVO;

import java.util.List;
import java.util.Map;

/**
 * 伤害计算器服务接口
 */
public interface DamageCalculatorService {
    
    /**
     * 计算伤害
     * @param request 伤害计算请求
     * @return 伤害计算结果
     */
    DamageResultVO calculateDamage(DamageCalculationRequest request);
    
    /**
     * 获取属性相性表
     * @return 属性相性矩阵 Map<攻击属性ID, Map<防御属性ID, 倍率>>
     */
    Map<Integer, Map<Integer, Integer>> getTypeEfficacyMatrix();
    
    /**
     * 获取特定攻击属性对所有防御属性的相性
     * @param damageTypeId 攻击属性ID
     * @return 属性相性列表
     */
    List<TypeEfficacyVO> getTypeEfficacyByDamageType(Integer damageTypeId);
    
    /**
     * 计算技能对目标属性的相性倍率
     * @param moveTypeId 技能属性ID
     * @param targetTypeIds 目标属性ID列表
     * @return 总倍率 (如: 4.0, 2.0, 1.0, 0.5, 0.25, 0.0)
     */
    Double calculateTypeEffectiveness(Integer moveTypeId, List<Integer> targetTypeIds);
}
