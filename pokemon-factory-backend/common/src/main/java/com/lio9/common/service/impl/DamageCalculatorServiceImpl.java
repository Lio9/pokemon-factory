package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lio9.common.mapper.TypeEfficacyMapper;
import com.lio9.common.service.DamageCalculatorService;
import com.lio9.common.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 伤害计算器服务实现
 */
@Service
public class DamageCalculatorServiceImpl implements DamageCalculatorService {

    @Autowired
    private TypeEfficacyMapper typeEfficacyMapper;

    @Override
    public DamageResultVO calculateDamage(DamageCalculationRequest request) {
        DamageResultVO result = new DamageResultVO();
        
        // 简化的伤害计算
        // TODO: 实现完整的伤害计算逻辑
        
        result.setMinDamage(10);
        result.setMaxDamage(20);
        result.setAvgDamage(15.0);
        result.setBaseDamage(15);
        result.setTypeEffectiveness(1.0);
        result.setEffectivenessDesc("1x");
        result.setIsStab(false);
        result.setStabMultiplier(1.0);
        
        return result;
    }

    @Override
    public Map<Integer, Map<Integer, Integer>> getTypeEfficacyMatrix() {
        // 简化实现，返回空的矩阵
        // TODO: 实现完整的属性相性矩阵查询
        return new HashMap<>();
    }

    @Override
    public List<TypeEfficacyVO> getTypeEfficacyByDamageType(Integer damageTypeId) {
        List<Map<String, Object>> efficacyList = typeEfficacyMapper.selectByDamageTypeId(damageTypeId);
        
        return efficacyList.stream().map(e -> {
            TypeEfficacyVO vo = new TypeEfficacyVO();
            vo.setDamageTypeId((Integer) e.get("damage_type_id"));
            vo.setDamageTypeName((String) e.get("damage_type_name"));
            vo.setTargetTypeId((Integer) e.get("target_type_id"));
            vo.setTargetTypeName((String) e.get("target_type_name"));
            vo.setDamageFactor((Integer) e.get("damage_factor"));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Double calculateTypeEffectiveness(Integer moveTypeId, List<Integer> targetTypeIds) {
        if (targetTypeIds == null || targetTypeIds.isEmpty()) {
            return 1.0;
        }
        
        Double multiplier = 1.0;
        
        for (Integer targetTypeId : targetTypeIds) {
            Integer factor = typeEfficacyMapper.selectDamageFactor(moveTypeId, targetTypeId);
            if (factor != null) {
                multiplier *= (factor / 100.0);
            }
        }
        
        return multiplier;
    }
}