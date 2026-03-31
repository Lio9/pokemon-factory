package com.lio9.pokedex.service.calculator;

import com.lio9.common.vo.DamageCalculationRequest;
import com.lio9.common.vo.DamageResultVO;
import com.lio9.common.vo.TypeEfficacyVO;
import java.util.List;
import java.util.Map;

/**
 * 伤害计算器服务
 */
public interface DamageCalculatorService {
    
    /**
     * 计算伤害
     */
    DamageResultVO calculateDamage(DamageCalculationRequest request);
    
    /**
     * 获取属性相性矩阵
     */
    Map<Integer, Map<Integer, Integer>> getTypeEfficacyMatrix();
    
    /**
     * 根据攻击属性获取属性相性
     */
    List<TypeEfficacyVO> getTypeEfficacyByDamageType(Integer damageTypeId);
    
    /**
     * 获取两个属性之间的相性
     */
    Integer getDamageFactor(Integer damageTypeId, Integer targetTypeId);
}