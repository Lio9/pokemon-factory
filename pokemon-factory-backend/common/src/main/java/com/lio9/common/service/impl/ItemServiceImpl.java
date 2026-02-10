package com.lio9.common.service.impl;

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
 * 创建人: Lio9
 */
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements ItemService {
    
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