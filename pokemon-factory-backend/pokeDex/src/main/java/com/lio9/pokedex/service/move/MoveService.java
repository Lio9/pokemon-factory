package com.lio9.pokedex.service.move;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Move;
import com.lio9.common.vo.MoveVO;
import java.util.List;

/**
 * 技能服务
 * 统一的技能业务逻辑
 */
public interface MoveService {
    
    /**
     * 分页查询技能列表
     */
    Page<Move> getMoveList(Integer current, Integer size, String name);
    
    /**
     * 根据ID获取技能详情
     */
    MoveVO getMoveDetail(Integer moveId);
    
    /**
     * 搜索技能
     */
    List<Move> searchMoves(String keyword);
    
    /**
     * 统计技能数量
     */
    long count();
    
    /**
     * 获取宝可梦形态的可学技能
     */
    List<MoveVO> getFormMoves(Integer formId);
    
    /**
     * 获取宝可梦的可学技能
     */
    List<MoveVO> getSpeciesMoves(Long speciesId);
}