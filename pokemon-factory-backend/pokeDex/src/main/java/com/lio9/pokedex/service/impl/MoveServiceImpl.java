package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.MoveMapper;
import com.lio9.common.model.Move;
import com.lio9.common.service.MoveService;
import com.lio9.common.vo.MoveQueryVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 技能服务实现类
 * 提供技能数据的分页查询和管理功能
 * 继承MyBatis-Plus的ServiceImpl，实现MoveService接口
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
 */
@Service
public class MoveServiceImpl extends ServiceImpl<MoveMapper, Move> implements MoveService {
    
    /**
     * 获取技能分页列表
     * 根据分页参数和查询条件获取技能数据
     * 
     * @param page 分页参数
     * @param vo 查询条件对象
     * @return 技能分页数据
     */
    @Override
    public Page<Move> getMovePage(Page<Move> page, MoveQueryVO vo) {
        // 简化实现，实际项目中应该根据查询条件进行筛选
        return this.page(page);
    }
    
    /**
     * 搜索技能
     * 根据关键词搜索技能数据
     * 
     * @param keyword 搜索关键词
     * @param page 分页参数
     * @return 匹配的技能列表
     */
    @Override
    public List<Move> searchMoves(String keyword, Page<Move> page) {
        // 简化实现
        return this.list();
    }
}