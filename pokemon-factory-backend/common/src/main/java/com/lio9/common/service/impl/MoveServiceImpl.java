package com.lio9.common.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.MoveMapper;
import com.lio9.common.model.Move;
import com.lio9.common.service.MoveService;
import com.lio9.common.vo.MoveQueryVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 技能服务实现
 */
@Service
public class MoveServiceImpl extends ServiceImpl<MoveMapper, Move> implements MoveService {

    @Override
    public Page<Move> getMovePage(Page<Move> page, MoveQueryVO queryVO) {
        QueryWrapper<Move> wrapper = new QueryWrapper<>();
        
        if (queryVO != null) {
            if (queryVO.getName() != null && !queryVO.getName().isEmpty()) {
                wrapper.and(w -> w.like("name", queryVO.getName()).or().like("name_en", queryVO.getName()));
            }
        }
        
        wrapper.orderByAsc("id");
        return page(page, wrapper);
    }

    @Override
    public List<Move> searchMoves(String keyword, Page<Move> page) {
        QueryWrapper<Move> wrapper = new QueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("name", keyword).or().like("name_en", keyword));
        }
        
        wrapper.orderByAsc("id");
        return list(wrapper);
    }
}