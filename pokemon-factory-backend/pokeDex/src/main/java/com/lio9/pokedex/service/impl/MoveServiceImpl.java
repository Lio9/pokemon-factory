package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.Move;
import com.lio9.pokedex.mapper.MoveMapper;
import com.lio9.pokedex.service.MoveService;
import com.lio9.pokedex.vo.MoveQueryVO;
import com.lio9.pokedex.vo.MoveVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    
    @Override
    public MoveVO getMoveDetail(Integer moveId) {
        Map<String, Object> moveDetail = baseMapper.selectMoveDetailById(moveId);
        if (moveDetail == null) {
            return null;
        }
        
        MoveVO vo = new MoveVO();
        vo.setId((Integer) moveDetail.get("id"));
        vo.setName((String) moveDetail.get("name"));
        vo.setNameEn((String) moveDetail.get("name_en"));
        vo.setTypeName((String) moveDetail.get("type_name"));
        vo.setTypeColor((String) moveDetail.get("type_color"));
        vo.setDamageClass((String) moveDetail.get("damage_class_name"));
        vo.setPower((Integer) moveDetail.get("power"));
        vo.setAccuracy((Integer) moveDetail.get("accuracy"));
        vo.setPp((Integer) moveDetail.get("pp"));
        vo.setPriority((Integer) moveDetail.get("priority"));
        vo.setDescription((String) moveDetail.get("description"));
        
        determineMoveProperties(vo, moveId);
        
        return vo;
    }
    
    @Override
    public List<MoveVO> getMoveDetailsByIds(List<Integer> moveIds) {
        if (moveIds == null || moveIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return moveIds.stream()
            .map(this::getMoveDetail)
            .filter(detail -> detail != null)
            .collect(Collectors.toList());
    }
    
    /**
     * 判断技能的属性（是否接触、连续攻击等）
     */
    private void determineMoveProperties(MoveVO vo, Integer moveId) {
        java.util.Set<Integer> contactMoves = new java.util.HashSet<>(java.util.Arrays.asList(
            1, 5, 7, 8, 9, 10, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
            31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
            57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100
        ));
        
        java.util.Map<Integer, Integer> multiHitMoves = new java.util.HashMap<>();
        multiHitMoves.put(540, 5);
        multiHitMoves.put(179, 5);
        multiHitMoves.put(333, 5);
        multiHitMoves.put(11, 5);
        multiHitMoves.put(542, 5);
        multiHitMoves.put(543, 5);
        multiHitMoves.put(544, 5);
        multiHitMoves.put(331, 5);
        multiHitMoves.put(468, 5);
        multiHitMoves.put(541, 5);
        multiHitMoves.put(546, 5);
        multiHitMoves.put(547, 5);
        multiHitMoves.put(548, 5);
        multiHitMoves.put(549, 5);
        multiHitMoves.put(550, 5);
        multiHitMoves.put(551, 5);
        multiHitMoves.put(552, 5);
        multiHitMoves.put(553, 5);
        multiHitMoves.put(554, 5);
        multiHitMoves.put(555, -2);
        multiHitMoves.put(556, -3);
        
        java.util.Map<Integer, Integer> recoilMoves = new java.util.HashMap<>();
        recoilMoves.put(1, 33);
        recoilMoves.put(7, 33);
        recoilMoves.put(8, -1);
        recoilMoves.put(9, 50);
        recoilMoves.put(10, -1);
        recoilMoves.put(14, -1);
        recoilMoves.put(15, -1);
        recoilMoves.put(16, -1);
        recoilMoves.put(17, -1);
        recoilMoves.put(18, -1);
        
        vo.setIsContact(contactMoves.contains(moveId));
        
        if (multiHitMoves.containsKey(moveId)) {
            Integer hits = multiHitMoves.get(moveId);
            if (hits != null && hits > 0) {
                vo.setHits(hits);
            } else {
                vo.setHits(5);
            }
        }
        
        if (recoilMoves.containsKey(moveId)) {
            Integer recoil = recoilMoves.get(moveId);
            if (recoil != null && recoil > 0) {
                vo.setRecoil(recoil);
            } else {
                vo.setRecoil(50);
            }
        }
        
        if (vo.getHits() != null && vo.getHits() > 1) {
            vo.setMoveCategory("连续攻击");
        } else if (vo.getRecoil() != null && vo.getRecoil() > 0) {
            vo.setMoveCategory("反伤技能");
        } else if (vo.getPriority() != null && vo.getPriority() != 0) {
            vo.setMoveCategory("先制技能");
        } else if (vo.getPower() == null || vo.getPower() == 0) {
            vo.setMoveCategory("变化技能");
        } else {
            vo.setMoveCategory("普通攻击");
        }
    }
}
