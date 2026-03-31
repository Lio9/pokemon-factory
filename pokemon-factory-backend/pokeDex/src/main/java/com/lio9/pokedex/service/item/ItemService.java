package com.lio9.pokedex.service.item;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Item;
import com.lio9.common.vo.ItemVO;
import java.util.List;

/**
 * 物品服务
 */
public interface ItemService {
    
    /**
     * 分页查询物品列表
     */
    Page<Item> getItemList(Integer current, Integer size, String name);
    
    /**
     * 根据ID获取物品详情
     */
    ItemVO getItemDetail(Integer itemId);
    
    /**
     * 搜索物品
     */
    List<Item> searchItems(String keyword);
    
    /**
     * 统计物品数量
     */
    long count();
    
    /**
     * 获取战斗物品列表
     */
    List<Item> getBattleItems();
}