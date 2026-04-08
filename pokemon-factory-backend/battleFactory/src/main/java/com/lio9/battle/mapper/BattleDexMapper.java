package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BattleDexMapper {
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

    @Select("SELECT stat_id, base_stat FROM pokemon_form_stat WHERE form_id = #{formId}")
    List<Map<String, Object>> selectFormStats(@Param("formId") Integer formId);

    @Select("SELECT pft.slot, t.id AS type_id, t.name, t.name_en FROM pokemon_form_type pft " +
            "JOIN type t ON t.id = pft.type_id " +
            "WHERE pft.form_id = #{formId} ORDER BY pft.slot")
    List<Map<String, Object>> selectFormTypes(@Param("formId") Integer formId);

    @Select("SELECT pfa.slot, pfa.is_hidden, a.id AS ability_id, a.name, a.name_en " +
            "FROM pokemon_form_ability pfa " +
            "JOIN ability a ON a.id = pfa.ability_id " +
            "WHERE pfa.form_id = #{formId} ORDER BY pfa.is_hidden ASC, pfa.slot ASC")
    List<Map<String, Object>> selectFormAbilities(@Param("formId") Integer formId);

    @Select("SELECT m.id, m.name, m.name_en, m.type_id, m.damage_class_id, m.target_id, " +
            "COALESCE(m.power, 0) AS power, COALESCE(m.accuracy, 100) AS accuracy, COALESCE(m.priority, 0) AS priority, " +
            "COALESCE(m.effect_short, '') AS effect_short " +
            "FROM pokemon_form_move pfm " +
            "JOIN move m ON m.id = pfm.move_id " +
            "WHERE pfm.form_id = #{formId} " +
            "AND (m.name_en = 'protect' OR (m.damage_class_id IN (1, 2) AND COALESCE(m.power, 0) > 0)) " +
            "GROUP BY m.id, m.name, m.name_en, m.type_id, m.damage_class_id, m.target_id, m.power, m.accuracy, m.priority, m.effect_short " +
            "ORDER BY CASE WHEN m.name_en = 'protect' THEN 0 ELSE 1 END, COALESCE(m.power, 0) DESC, COALESCE(m.accuracy, 100) DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectCompetitiveMoves(@Param("formId") Integer formId, @Param("limit") int limit);
}
