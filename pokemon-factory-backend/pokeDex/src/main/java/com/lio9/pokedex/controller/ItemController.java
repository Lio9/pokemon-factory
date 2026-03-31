package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Item;
import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
import com.lio9.common.service.ItemService;
import com.lio9.common.vo.ItemQueryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 物品控制器
 * 创建人: Lio9
 */
@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
public class ItemController {
    
    @Autowired
    private ItemService itemService;
    
    /**
     * 分页获取物品列表
     */
    @GetMapping("/list")
    public Map<String, Object> getItemList(ItemQueryVO queryVO) {
        Page<Item> page = new Page<>(queryVO.getCurrent(), queryVO.getSize());
        Page<Item> itemPage = itemService.page(page);
        
        return ResultResponse.buildPageSuccess(itemPage);
    }
    
    /**
     * 获取物品详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getItemDetail(@PathVariable Long id) {
        Item item = itemService.getById(id);
        
        if (item != null) {
            return ResultResponse.buildSuccess("success", item);
        } else {
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
        
        Page<Item> page = new Page<>(current, size);
        Page<Item> itemPage = itemService.page(page);
        
        return ResultResponse.buildPageSuccess(itemPage);
    }
    
    /**
     * 批量导入物品数据
     */
    @PostMapping("/import")
    public Map<String, Object> importItems(@RequestBody List<Item> items) {
        try {
            if (items == null || items.isEmpty()) {
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
            
            return ResultResponse.buildImportSuccess(importResult);
        } catch (Exception e) {
            return ResultResponse.buildImportFailed(e.getMessage());
        }
    }
    
    /**
     * 导入单个物品
     */
    @PostMapping("/import-single")
    public Map<String, Object> importSingleItem(@RequestBody Item item) {
        try {
            if (item == null) {
                return ResultResponse.buildCustomErrorResponse(ResponseCode.BAD_REQUEST, "物品数据不能为空", null);
            }
            
            if (itemService.save(item)) {
                return ResultResponse.buildOperationSuccess(item);
            } else {
                return ResultResponse.buildOperationFailed("导入失败");
            }
        } catch (Exception e) {
            return ResultResponse.buildImportFailed(e.getMessage());
        }
    }
}