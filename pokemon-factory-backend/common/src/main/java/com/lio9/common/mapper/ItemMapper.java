package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

/**
 * 物品Mapper接口
 */
@Mapper
public interface ItemMapper extends BaseMapper<Item> {
    
    /**
     * 搜索物品
     */
    @Select("SELECT * FROM item WHERE name LIKE CONCAT('%', #{keyword}, '%') OR name_en LIKE CONCAT('%', #{keyword}, '%') ORDER BY id")
    List<Item> searchItems(@Param("keyword") String keyword);
}
