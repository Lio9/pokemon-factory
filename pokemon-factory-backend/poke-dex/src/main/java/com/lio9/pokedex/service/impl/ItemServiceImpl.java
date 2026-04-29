package com.lio9.pokedex.service.impl;



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
