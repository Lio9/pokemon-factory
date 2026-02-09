package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 蛋组实体类
 */
@Data
@TableName("egg_group")
public class EggGroup {
    /**
     * 蛋组ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 蛋组名称
     */
    private String name;
    
    /**
     * 蛋组英文名称
     */
    private String nameEn;
    
    /**
     * 蛋组日文名称
     */
    private String nameJp;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}