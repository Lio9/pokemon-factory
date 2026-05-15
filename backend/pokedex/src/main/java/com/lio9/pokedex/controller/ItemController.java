package com.lio9.pokedex.controller;



import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.pokedex.model.Item;
import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
import com.lio9.pokedex.service.ItemService;
import com.lio9.pokedex.vo.ItemQueryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 物品控制器
 * 创建人: Lio9
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {
    
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }
    
    /**
     * 分页获取物品列表
     */
    @GetMapping("/list")
    public Map<String, Object> getItemList(ItemQueryVO queryVO) {
        long startTime = System.currentTimeMillis();
        logger.info("获取物品列表 - 参数: current={}, size={}", queryVO.getCurrent(), queryVO.getSize());
        
        Page<Item> page = new Page<>(queryVO.getCurrent(), queryVO.getSize());
        Page<Item> itemPage = itemService.page(page);
        
        long endTime = System.currentTimeMillis();
        logger.info("获取物品列表成功 - 耗时: {}ms, 总数: {}", (endTime - startTime), itemPage.getTotal());
        
        return ResultResponse.buildPageSuccess(itemPage);
    }
    
    /**
     * 获取物品详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getItemDetail(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        logger.info("获取物品详情 - ID: {}", id);
        
        Item item = itemService.getById(id);
        
        long endTime = System.currentTimeMillis();
        if (item != null) {
            logger.info("获取物品详情成功 - 耗时: {}ms", (endTime - startTime));
            return ResultResponse.buildSuccess("success", item);
        } else {
            logger.warn("获取物品详情失败 - 找不到ID为{}的物品, 耗时: {}ms", id, (endTime - startTime));
            return ResultResponse.buildNotFound("物品", id);
        }
    }
    
    /**
     * 搜索物品
     */
    @GetMapping("/search")
    public Map<String, Object> searchItems(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        long startTime = System.currentTimeMillis();
        logger.info("搜索物品 - 关键词: {}, current={}, size={}", keyword, current, size);
        
        Page<Item> page = new Page<>(current, size);
        Page<Item> itemPage = itemService.page(page);
        
        long endTime = System.currentTimeMillis();
        logger.info("搜索物品成功 - 耗时: {}ms, 总数: {}", (endTime - startTime), itemPage.getTotal());
        
        return ResultResponse.buildPageSuccess(itemPage);
    }
    
    /**
     * 批量导入物品数据
     */
    @PostMapping("/import")
    public Map<String, Object> importItems(@RequestBody List<Item> items) {
        long startTime = System.currentTimeMillis();
        logger.info("批量导入物品数据 - 物品数量: {}", items != null ? items.size() : 0);
        
        try {
            if (items == null || items.isEmpty()) {
                long endTime = System.currentTimeMillis();
                logger.warn("批量导入物品数据失败 - 导入数据不能为空, 耗时: {}ms", (endTime - startTime));
                return ResultResponse.buildCustomErrorResponse(ResponseCode.BAD_REQUEST, "导入数据不能为空", null);
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (Item item : items) {
                try {
                    if (itemService.save(item)) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                }
            }
            
            Map<String, Object> importResult = Map.of(
                "total", items.size(),
                "success", successCount,
                "fail", failCount
            );
            
            long endTime = System.currentTimeMillis();
            logger.info("批量导入物品数据成功 - 总数: {}, 成功: {}, 失败: {}, 耗时: {}ms", 
                    items.size(), successCount, failCount, (endTime - startTime));
            
            return ResultResponse.buildImportSuccess(importResult);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            logger.error("批量导入物品数据失败 - 耗时: {}ms, 错误: {}", (endTime - startTime), e.getMessage());
            return ResultResponse.buildImportFailed(e.getMessage());
        }
    }
    
    /**
     * 导入单个物品
     */
    @PostMapping("/import-single")
    public Map<String, Object> importSingleItem(@RequestBody Item item) {
        long startTime = System.currentTimeMillis();
        logger.info("导入单个物品 - 物品名称: {}", item != null ? item.getName() : "null");
        
        try {
            if (item == null) {
                long endTime = System.currentTimeMillis();
                logger.warn("导入单个物品失败 - 物品数据不能为空, 耗时: {}ms", (endTime - startTime));
                return ResultResponse.buildCustomErrorResponse(ResponseCode.BAD_REQUEST, "物品数据不能为空", null);
            }
            
            if (itemService.save(item)) {
                long endTime = System.currentTimeMillis();
                logger.info("导入单个物品成功 - 物品名称: {}, 耗时: {}ms", item.getName(), (endTime - startTime));
                return ResultResponse.buildOperationSuccess(item);
            } else {
                long endTime = System.currentTimeMillis();
                logger.warn("导入单个物品失败 - 物品名称: {}, 耗时: {}ms", item.getName(), (endTime - startTime));
                return ResultResponse.buildOperationFailed("导入失败");
            }
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            logger.error("导入单个物品失败 - 物品名称: {}, 耗时: {}ms, 错误: {}", 
                    item != null ? item.getName() : "null", (endTime - startTime), e.getMessage());
            return ResultResponse.buildImportFailed(e.getMessage());
        }
    }
}
