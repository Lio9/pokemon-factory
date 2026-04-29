package com.lio9.pokedex.service;



import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lio9.pokedex.model.Item;

import java.util.List;

/**
 * 物品服务接口
 */
public interface ItemService extends IService<Item> {
    
    /**
     * 搜索物品
     */
    Page<Item> searchItems(String keyword, Page<Item> page);
    
    /**
     * 根据分类获取物品
     */
    List<Item> getItemsByCategory(Integer categoryId);
}
