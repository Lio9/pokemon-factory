package com.lio9.pokedex.service.impl;



/**
 * ItemServiceImpl 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端业务实现文件。
 * 核心职责：负责落地具体业务流程，通常会组合 Mapper、工具类与领域模型。
 * 阅读建议：建议关注事务边界、核心分支和依赖协作。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.pokedex.model.Item;
import com.lio9.pokedex.mapper.ItemMapper;
import com.lio9.pokedex.service.ItemService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 物品服务实现
 */
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements ItemService {

    @Override
    public Page<Item> searchItems(String keyword, Page<Item> page) {
        QueryWrapper<Item> wrapper = new QueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("name", keyword).or().like("name_en", keyword));
        }
        
        wrapper.orderByAsc("id");
        return page(page, wrapper);
    }

    @Override
    public List<Item> getItemsByCategory(Integer categoryId) {
        QueryWrapper<Item> wrapper = new QueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        wrapper.orderByAsc("id");
        return list(wrapper);
    }
}
