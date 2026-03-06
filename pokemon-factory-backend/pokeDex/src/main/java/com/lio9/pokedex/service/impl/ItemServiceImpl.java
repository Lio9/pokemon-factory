package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.ItemMapper;
import com.lio9.common.model.Item;
import com.lio9.common.service.ItemService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 物品服务实现类
 */
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements ItemService {

    @Override
    public Page<Item> searchItems(String keyword, Page<Item> page) {
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", keyword)
                   .or()
                   .like("name_en", keyword)
                   .orderByAsc("id");
        return this.page(page, queryWrapper);
    }

    @Override
    public List<Item> getItemsByCategory(Integer categoryId) {
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id", categoryId).orderByAsc("id");
        return this.list(queryWrapper);
    }
}
