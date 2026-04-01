package com.lio9.common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lio9.common.model.Move;
import com.lio9.common.vo.MoveQueryVO;
import com.lio9.common.vo.MoveVO;

import java.util.List;
import java.util.Map;

/**
 * 技能服务接口
 * 创建人: Lio9
 */
public interface MoveService extends IService<Move> {
    
    /**
     * 分页查询技能列表
     */
    Page<Move> getMovePage(Page<Move> page, MoveQueryVO vo);
    
    /**
     * 搜索技能
     */
    List<Move> searchMoves(String keyword, Page<Move> page);
    
    /**
     * 获取技能详情（包含完整信息）
     */
    MoveVO getMoveDetail(Integer moveId);
    
    /**
     * 根据技能ID列表批量获取技能详情
     */
    List<MoveVO> getMoveDetailsByIds(List<Integer> moveIds);
}