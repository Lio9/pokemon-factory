package com.lio9.battle.effect;

/**
 * 宝可梦 18 种属性与伤害分类枚举。
 * <p>对应 PokeAPI/数据库 type 表的属性 ID 体系（PokeAPI 编号）。</p>
 * <p>注意：PokeAPI 编号与游戏内排序不同，例如 fire=10 而非 2。</p>
 */
public enum PokemonType {

    NORMAL(1),
    FIGHTING(2),
    FLYING(3),
    POISON(4),
    GROUND(5),
    ROCK(6),
    BUG(7),
    GHOST(8),
    STEEL(9),
    FIRE(10),
    WATER(11),
    GRASS(12),
    ELECTRIC(13),
    PSYCHIC(14),
    ICE(15),
    DRAGON(16),
    DARK(17),
    FAIRY(18);

    private final int id;

    PokemonType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static PokemonType fromId(int id) {
        for (PokemonType t : values()) {
            if (t.id == id) return t;
        }
        return null;
    }

    /** 伤害分类 */
    public static final int PHYSICAL = 1;
    public static final int SPECIAL = 2;
}
