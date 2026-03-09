package com.lio9.common.service;

import com.lio9.common.vo.DamageCalculationRequest;
import com.lio9.common.vo.DamageResultVO;
import com.lio9.common.vo.TypeEfficacyVO;

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
