package com.lio9.battle.effect;

/**
 * 宝可梦 18 种属性与伤害分类枚举。
 * <p>对应 Showdown/PokeAPI 的属性 ID 体系。</p>
 */
public enum PokemonType {

    NORMAL(1),
    FIRE(2),
    WATER(3),
    ELECTRIC(4),
    GRASS(5),
    ICE(6),
    FIGHTING(7),
    POISON(8),
    GROUND(9),
    FLYING(10),
    PSYCHIC(11),
    BUG(12),
    ROCK(13),
    GHOST(14),
    DRAGON(15),
    DARK(16),
    STEEL(17),
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
