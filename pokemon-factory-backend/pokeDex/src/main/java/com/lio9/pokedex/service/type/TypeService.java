package com.lio9.pokedex.service.type;

import com.lio9.common.model.Type;
import com.lio9.common.vo.TypeVO;
import java.util.List;

/**
 * 属性服务
 */
public interface TypeService {
    
    /**
     * 获取所有属性
     */
    List<TypeVO> getAllTypes();
    
    /**
     * 根据ID获取属性详情
     */
    TypeVO getTypeDetail(Integer typeId);
    
    /**
     * 获取属性相性矩阵
     */
    List<TypeVO> getTypeEfficacyMatrix(Integer damageTypeId);
    
    /**
     * 获取两个属性之间的相性
     */
    Integer getDamageFactor(Integer damageTypeId, Integer targetTypeId);
    
    /**
     * 统计属性数量
     */
    long count();
}