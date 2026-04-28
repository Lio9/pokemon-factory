package com.lio9.pokedex.service;



/**
 * MoveService 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端业务服务文件。
 * 核心职责：负责定义或承载模块级业务能力，对上层暴露稳定服务接口。
 * 阅读建议：建议结合控制器和实现类一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lio9.pokedex.model.Move;
import com.lio9.pokedex.vo.MoveQueryVO;
import com.lio9.pokedex.vo.MoveVO;

import java.util.List;

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
