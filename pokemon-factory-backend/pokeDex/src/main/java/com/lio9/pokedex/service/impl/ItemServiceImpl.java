package com.lio9.pokedex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lio9.common.mapper.ItemMapper;
import com.lio9.common.model.Item;
import com.lio9.common.service.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 物品服务实现类
 * 提供物品数据的批量导入和JSON导入功能
 * 继承MyBatis-Plus的ServiceImpl，实现ItemService接口
 * 
 * @author Lio9
 * @version 1.0
 * @since 2024-01-01
 */
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements ItemService {
    
    /**
     * 批量导入物品数据
     * 使用MyBatis-Plus的saveBatch方法进行批量插入
     * 
     * @param items 物品列表，不能为null或空
     * @return 导入是否成功
     */
    @Override
    @Transactional
    public boolean batchImport(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        
        try {
            return this.saveBatch(items);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从JSON文件导入物品数据
     * 读取指定路径的JSON文件，解析内容并批量导入
     * 
     * @param filePath JSON文件路径，不能为null或空
     * @return 导入是否成功
     */
    @Override
    @Transactional
    public boolean importFromJson(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return false;
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            
            List<Item> items = parseJson(content.toString());
            return batchImport(items);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从CSV文件导入物品数据
     * 读取指定路径的CSV文件，解析内容并批量导入
     * CSV格式：name,name_en,name_jp,category,price,effetct[,description]
     * 
     * @param filePath CSV文件路径，不能为null或空
     * @return 导入是否成功
     */
    @Override
    @Transactional
    public boolean importFromCsv(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return false;
            }
            
            List<Item> items = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (header == null) {
                    return false;
                }
                
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");
                    if (values.length >= 6) {
                        Item item = new Item();
                        item.setName(values[0].trim());
                        item.setNameEn(values[1].trim());
                        item.setNameJp(values[2].trim());
                        item.setCategory(values[3].trim());
                        item.setPrice(Integer.parseInt(values[4].trim()));
                        item.setEffect(values[5].trim());
                        if (values.length > 6) {
                            item.setDescription(values[6].trim());
                        }
                        items.add(item);
                    }
                }
            }
            
            return batchImport(items);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 解析JSON字符串为物品列表
     * 处理JSON格式的字符串，提取物品信息并创建Item对象
     * 
     * @param json JSON字符串
     * @return 物品列表
     */
    private List<Item> parseJson(String json) {
        List<Item> items = new ArrayList<>();
        try {
            json = json.trim();
            if (json.startsWith("[")) {
                json = json.substring(1, json.length() - 1);
            }
            
            String[] itemStrings = json.split("\\},\\s*\\{");
            for (String itemStr : itemStrings) {
                Item item = new Item();
                itemStr = itemStr.trim();
                if (itemStr.startsWith("{")) {
                    itemStr = itemStr.substring(1);
                }
                if (itemStr.endsWith("}")) {
                    itemStr = itemStr.substring(0, itemStr.length() - 1);
                }
                
                String[] pairs = itemStr.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replaceAll("\"", "");
                        String value = keyValue[1].trim().replaceAll("\"", "");
                        if (value.startsWith("{") && value.endsWith("}")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        
                        switch (key) {
                            case "name":
                                item.setName(value);
                                break;
                            case "nameEn":
                                item.setNameEn(value);
                                break;
                            case "nameJp":
                                item.setNameJp(value);
                                break;
                            case "category":
                                item.setCategory(value);
                                break;
                            case "price":
                                try {
                                    item.setPrice(Integer.parseInt(value));
                                } catch (NumberFormatException e) {
                                    item.setPrice(0);
                                }
                                break;
                            case "effect":
                                item.setEffect(value);
                                break;
                            case "description":
                                item.setDescription(value);
                                break;
                        }
                    }
                }
                items.add(item);
            }
        } catch (Exception e) {
            items.clear();
        }
        return items;
    }
}