package com.lio9.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lio9.common.model.Item;

import java.util.List;

/**
 * 物品服务接口
 * 创建人: Lio9
 */
public interface ItemService extends IService<Item> {
    
    /**
     * 批量导入物品数据
     */
    boolean batchImport(List<Item> items);
    
    /**
     * 从JSON文件导入物品数据
     */
    boolean importFromJson(String filePath);
    
    /**
     * 从CSV文件导入物品数据
     */
    boolean importFromCsv(String filePath);
}