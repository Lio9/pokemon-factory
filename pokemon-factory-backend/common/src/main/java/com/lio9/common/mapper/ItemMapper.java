package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.Item;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物品Mapper接口
 * 创建人: Lio9
 */
@Mapper
public interface ItemMapper extends BaseMapper<Item> {
    
}