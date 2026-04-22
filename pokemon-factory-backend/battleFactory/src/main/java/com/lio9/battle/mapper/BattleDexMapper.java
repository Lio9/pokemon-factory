package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 对战工厂图鉴查询 Mapper。
 * <p>
 * 该接口专门面向 battleFactory 的 AI 组队场景，
 * 从 common 下沉后的图鉴公共表中挑选候选宝可梦、种族值、属性、特性和招式。
 * </p>
 */
@Mapper
public interface BattleDexMapper {
    /**
     * 随机抽取可用于工厂组队的默认形态。
     */
    @Select("SELECT pf.id AS form_id, ps.id AS species_id, COALESCE(ps.name_en, ps.name) AS name_en, ps.name AS name, pf.base_experience, pf.official_artwork_url " +
            "FROM pokemon_form pf " +
            "JOIN pokemon_species ps ON ps.id = pf.species_id " +
            "WHERE pf.is_default = 1 " +
            "AND COALESCE(pf.is_battle_only, 0) = 0 " +
            "AND COALESCE(pf.is_mega, 0) = 0 " +
            "AND COALESCE(pf.is_gigantamax, 0) = 0 " +
            "AND COALESCE(pf.is_terastal, 0) = 0 " +
            "AND COALESCE(ps.is_baby, 0) = 0 " +
            "AND COALESCE(ps.is_mythical, 0) = 0 " +
            "AND COALESCE(ps.is_legendary, 0) = 0 " +
            "ORDER BY RANDOM() LIMIT #{limit}")
    List<Map<String, Object>> selectRandomDefaultForms(@Param("limit") int limit);

    /**
     * 查询指定形态的种族值。
     */
    @Select("SELECT stat_id, base_stat FROM pokemon_form_stat WHERE form_id = #{formId}")
    List<Map<String, Object>> selectFormStats(@Param("formId") Integer formId);

    /**
     * 查询指定形态的属性组合。
     */
    @Select("SELECT pft.slot, t.id AS type_id, t.name, t.name_en FROM pokemon_form_type pft " +
            "JOIN type t ON t.id = pft.type_id " +
            "WHERE pft.form_id = #{formId} ORDER BY pft.slot")
    List<Map<String, Object>> selectFormTypes(@Param("formId") Integer formId);

    /**
     * 查询指定形态可用特性。
     */
    @Select("SELECT pfa.slot, pfa.is_hidden, a.id AS ability_id, a.name, a.name_en " +
            "FROM pokemon_form_ability pfa " +
            "JOIN ability a ON a.id = pfa.ability_id " +
            "WHERE pfa.form_id = #{formId} ORDER BY pfa.is_hidden ASC, pfa.slot ASC")
    List<Map<String, Object>> selectFormAbilities(@Param("formId") Integer formId);

    /**
     * 查询 AI 组队阶段用于挑选的候选招式池。
     */
    @Select("SELECT m.id, m.name, m.name_en, m.type_id, m.damage_class_id, m.target_id, " +
            "COALESCE(m.power, 0) AS power, COALESCE(m.accuracy, 100) AS accuracy, COALESCE(m.priority, 0) AS priority, " +
            "COALESCE(mm.min_hits, 0) AS min_hits, COALESCE(mm.max_hits, 0) AS max_hits, COALESCE(mm.crit_rate, 0) AS crit_rate, " +
            "COALESCE(m.effect_chance, 0) AS effect_chance, COALESCE(mm.ailment_chance, 0) AS ailment_chance, COALESCE(mm.flinch_chance, 0) AS flinch_chance, " +
            "COALESCE(mm.stat_chance, 0) AS stat_chance, COALESCE(mm.drain, 0) AS drain, COALESCE(mm.healing, 0) AS healing, " +
            "COALESCE(ma.name_en, '') AS ailment_name_en, COALESCE(mc.name_en, '') AS category_name_en, " +
            "COALESCE(msc.stat_changes, '') AS stat_changes, COALESCE(mf.flags, '') AS flags, " +
            "COALESCE(m.effect_short, '') AS effect_short " +
            "FROM pokemon_form_move pfm " +
            "JOIN move m ON m.id = pfm.move_id " +
            "LEFT JOIN move_meta mm ON mm.move_id = m.id " +
            "LEFT JOIN move_meta_ailment ma ON ma.id = mm.ailment_id " +
            "LEFT JOIN move_meta_category mc ON mc.id = mm.category_id " +
            "LEFT JOIN (" +
            "SELECT move_id, GROUP_CONCAT(stat_id || ':' || \"change\", ',') AS stat_changes " +
            "FROM move_meta_stat_change GROUP BY move_id" +
            ") msc ON msc.move_id = m.id " +
            "LEFT JOIN (" +
            "SELECT mfm.move_id, GROUP_CONCAT(mf.identifier, ',') AS flags " +
            "FROM move_flag_map mfm JOIN move_flags mf ON mf.id = mfm.flag_id GROUP BY mfm.move_id" +
            ") mf ON mf.move_id = m.id " +
            "WHERE pfm.form_id = #{formId} " +
            "AND (" +
            "m.name_en IN (" +
            "'protect','detect','wide-guard','quick-guard','tailwind','trick-room','rain-dance','sunny-day','sandstorm','snowscape'," +
            "'electric-terrain','psychic-terrain','grassy-terrain','misty-terrain','reflect','light-screen','aurora-veil','safeguard','taunt','disable','heal-block','torment','encore','yawn','spore'," +
            "'helping-hand','follow-me','rage-powder','will-o-wisp','thunder-wave','icy-wind','electroweb','snarl','fake-tears'," +
            "'parting-shot','ally-switch','feint','recover','roost','slack-off','soft-boiled','moonlight','synthesis','morning-sun'" +
            ") OR (m.damage_class_id IN (1, 2) AND COALESCE(m.power, 0) > 0)" +
            ") " +
            "GROUP BY m.id, m.name, m.name_en, m.type_id, m.damage_class_id, m.target_id, m.power, m.accuracy, m.priority, " +
            "mm.min_hits, mm.max_hits, mm.crit_rate, m.effect_chance, mm.ailment_chance, mm.flinch_chance, mm.stat_chance, mm.drain, mm.healing, ma.name_en, mc.name_en, msc.stat_changes, mf.flags, m.effect_short " +
            "ORDER BY CASE WHEN m.name_en = 'protect' THEN 0 ELSE 1 END, COALESCE(m.power, 0) DESC, COALESCE(m.accuracy, 100) DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectCompetitiveMoves(@Param("formId") Integer formId, @Param("limit") int limit);
}
