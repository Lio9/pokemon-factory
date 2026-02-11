package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 进化链实体类
 */
@Data
@Accessors(chain = true)
@TableName("evolution_chain")
public class EvolutionChain {
    /**
     * 进化链ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 宝可梦ID
     */
    private Long pokemonId;

    /**
     * 进化自的宝可梦ID
     */
    private Long evolvesFromId;

    /**
     * 进化方式
     */
    private String evolutionMethod;

    /**
     * 进化参数
     */
    private String evolutionParameter;

    /**
     * 进化值
     */
    private String evolutionValue;

    /**
     * 进化链ID（从PokeAPI获取）
     */
    private Long chainId;

    /**
     * 进化详情（JSON格式）
     */
    private String evolutionDetails;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // 手动添加setter方法以确保编译通过
    public EvolutionChain setChainId(Long chainId) {
        this.chainId = chainId;
        return this;
    }

    public EvolutionChain setEvolutionDetails(String evolutionDetails) {
        this.evolutionDetails = evolutionDetails;
        return this;
    }

    public EvolutionChain setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public EvolutionChain setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvolutionChain that = (EvolutionChain) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}