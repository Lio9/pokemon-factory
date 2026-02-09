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
 */
@Service
public class MoveServiceImpl extends ServiceImpl<MoveMapper, Move> implements MoveService {
    
    @Override
    public Page<Move> getMovePage(Page<Move> page, MoveQueryVO vo) {
        // 简化实现，实际项目中应该根据查询条件进行筛选
        return this.page(page);
    }
    
    @Override
    public List<Move> searchMoves(String keyword, Page<Move> page) {
        // 简化实现
        return this.list();
    }
}