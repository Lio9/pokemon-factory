package com.lio9.pokedex.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lio9.common.model.Item;
import com.lio9.common.response.ResultResponse;
import com.lio9.common.response.ResponseCode;
import com.lio9.common.service.ItemService;
import com.lio9.common.vo.ItemQueryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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
        Map<String, Object> result = new HashMap<>();
        Page<Item> page = new Page<>(queryVO.getCurrent(), queryVO.getSize());
        Page<Item> itemPage = itemService.page(page);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", itemPage);
        return result;
    }
    
    /**
     * 获取物品详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getItemDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Item item = itemService.getById(id);
        
        if (item != null) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", item);
        } else {
            result.put("code", 404);
            result.put("message", "物品不存在");
        }
        return result;
    }
    
    /**
     * 搜索物品
     */
    @GetMapping("/search")
    public Map<String, Object> searchItems(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        
        Map<String, Object> result = new HashMap<>();
        Page<Item> page = new Page<>(current, size);
        Page<Item> itemPage = itemService.page(page);
        
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", itemPage);
        return result;
    }
    
    /**
     * 批量导入物品数据
     */
    @PostMapping("/import")
    public Map<String, Object> importItems(@RequestBody List<Item> items) {
        Map<String, Object> result = new HashMap<>();
        
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
            
            result.put("code", 200);
            result.put("message", "导入完成");
            result.put("data", Map.of(
                "total", items.size(),
                "success", successCount,
                "fail", failCount
            ));
        } catch (Exception e) {
            return ResultResponse.buildCustomErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, "导入失败: " + e.getMessage(), null);
        }
        
        return result;
    }
    
    /**
     * 导入单个物品
     */
    @PostMapping("/import-single")
    public Map<String, Object> importSingleItem(@RequestBody Item item) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (item == null) {
                result.put("code", 400);
                result.put("message", "物品数据不能为空");
                return result;
            }
            
            if (itemService.save(item)) {
                return ResultResponse.buildSuccessResponse(ResponseCode.OPERATION_SUCCESS, "导入成功", item);
            } else {
                return ResultResponse.buildCustomErrorResponse(ResponseCode.OPERATION_FAILED, "导入失败", null);
            }
        } catch (Exception e) {
            return ResultResponse.buildCustomErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, "导入失败: " + e.getMessage(), null);
        }
    }
}