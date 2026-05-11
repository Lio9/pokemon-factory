package com.lio9.pokedex.vo;

import java.util.List;

/**
 * 进化链树形节点。
 *
 * <p>替代扁平的 {@link EvolutionChainVO}，支持递归嵌套表达分支进化关系。
 * 例如伊布家族：一个根节点（伊布）携带 8 个子节点，每个子节点有独立的进化条件。</p>
 */
public class EvolutionNodeVO {
    private Integer speciesId;
    private String pokemonName;
    private String spriteUrl;
    private Boolean isCurrent;

    /** 进化触发条件中文描述（如 "升级"、"使用水之石"） */
    private String trigger;

    /** 进化所需等级（升级进化时有效） */
    private Integer minLevel;

    /** 进化所需物品（使用道具进化时有效） */
    private String item;

    /** 子节点（分支进化） */
    private List<EvolutionNodeVO> children;

    // ── getters / setters ─────────────────────────────────────────────

    public Integer getSpeciesId() { return speciesId; }
    public void setSpeciesId(Integer speciesId) { this.speciesId = speciesId; }

    public String getPokemonName() { return pokemonName; }
    public void setPokemonName(String pokemonName) { this.pokemonName = pokemonName; }

    public String getSpriteUrl() { return spriteUrl; }
    public void setSpriteUrl(String spriteUrl) { this.spriteUrl = spriteUrl; }

    public Boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }

    public String getTrigger() { return trigger; }
    public void setTrigger(String trigger) { this.trigger = trigger; }

    public Integer getMinLevel() { return minLevel; }
    public void setMinLevel(Integer minLevel) { this.minLevel = minLevel; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public List<EvolutionNodeVO> getChildren() { return children; }
    public void setChildren(List<EvolutionNodeVO> children) { this.children = children; }
}
