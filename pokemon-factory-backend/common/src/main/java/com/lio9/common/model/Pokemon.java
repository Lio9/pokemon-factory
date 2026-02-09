package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝可梦实体类
 */
@Data
@TableName("pokemon")
public class Pokemon {
    /**
     * 宝可梦ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 宝可梦全国图鉴编号
     */
    private String indexNumber;
    
    /**
     * 宝可梦名称(中文)
     */
    private String name;
    
    /**
     * 宝可梦名称(英文)
     */
    private String nameEn;
    
    /**
     * 宝可梦名称(日文)
     */
    private String nameJp;
    
    /**
     * 身高(米)
     */
    private double height;
    
    /**
     * 体重(公斤)
     */
    private double weight;
    
    /**
     * 基础经验值
     */
    private int baseExperience;
    
    /**
     * 基础亲密度
     */
    private int baseHappiness;
    
    /**
     * 捕获率
     */
    private int captureRate;
    
    /**
     * 性别比率(-1=无性别,0=全雄,8=全雌)
     */
    private int genderRate;
    
    /**
     * 进化链ID
     */
    private Long evolutionChainId;
    
    /**
     * 世代ID
     */
    private int generationId;
    
    /**
     * 排序
     */
    private int order;
    
    /**
     * 是否为婴儿宝可梦
     */
    private boolean isBaby;
    
    /**
     * 是否为传说宝可梦
     */
    private boolean isLegendary;
    
    /**
     * 是否为神话宝可梦
     */
    private boolean isMythical;
    
    /**
     * 宝可梦描述
     */
    private String profile;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    // 手动添加所有setter方法
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setIndexNumber(String indexNumber) {
        this.indexNumber = indexNumber;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }
    
    public void setNameJp(String nameJp) {
        this.nameJp = nameJp;
    }
    
    public void setHeight(double height) {
        this.height = height;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public void setBaseExperience(int baseExperience) {
        this.baseExperience = baseExperience;
    }
    
    public void setBaseHappiness(int baseHappiness) {
        this.baseHappiness = baseHappiness;
    }
    
    public void setCaptureRate(int captureRate) {
        this.captureRate = captureRate;
    }
    
    public void setGenderRate(int genderRate) {
        this.genderRate = genderRate;
    }
    
    public void setEvolutionChainId(Long evolutionChainId) {
        this.evolutionChainId = evolutionChainId;
    }
    
    public void setGenerationId(int generationId) {
        this.generationId = generationId;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public void setIsBaby(boolean isBaby) {
        this.isBaby = isBaby;
    }
    
    public void setIsLegendary(boolean isLegendary) {
        this.isLegendary = isLegendary;
    }
    
    public void setIsMythical(boolean isMythical) {
        this.isMythical = isMythical;
    }
    
    public void setProfile(String profile) {
        this.profile = profile;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}