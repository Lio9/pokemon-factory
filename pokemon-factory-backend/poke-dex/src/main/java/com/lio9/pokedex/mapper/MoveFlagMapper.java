package com.lio9.pokedex.mapper;



import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 技能标记Mapper接口
 * 用于查询技能的特殊标记（接触、拳击、咬、声音等）
 */
@Mapper
public interface MoveFlagMapper {
    
    int FLAG_CONTACT = 1;
    int FLAG_CHARGE = 2;
    int FLAG_RECHARGE = 3;
    int FLAG_PROTECT = 4;
    int FLAG_REFLECTABLE = 5;
    int FLAG_SNATCH = 6;
    int FLAG_MIRROR = 7;
    int FLAG_PUNCH = 8;
    int FLAG_SOUND = 9;
    int FLAG_GRAVITY = 10;
    int FLAG_DEFROST = 11;
    int FLAG_DISTANCE = 12;
    int FLAG_HEAL = 13;
    int FLAG_AUTHENTIC = 14;
    int FLAG_POWDER = 15;
    int FLAG_BITE = 16;
    int FLAG_PULSE = 17;
    int FLAG_BALLISTICS = 18;
    int FLAG_MENTAL = 19;
    int FLAG_NON_SKY_BATTLE = 20;
    int FLAG_DANCE = 21;
    
    /**
     * 获取技能的所有标记ID
     */
    @Select("SELECT flag_id FROM move_flag_map WHERE move_id = #{moveId}")
    List<Integer> selectFlagIdsByMoveId(@Param("moveId") Integer moveId);
    
    /**
     * 检查技能是否具有特定标记
     */
    @Select("SELECT COUNT(*) > 0 FROM move_flag_map WHERE move_id = #{moveId} AND flag_id = #{flagId}")
    boolean hasFlag(@Param("moveId") Integer moveId, @Param("flagId") Integer flagId);
    
    /**
     * 获取技能的所有标记详情
     */
    @Select("SELECT mf.id, mf.identifier, mf.name, mf.description " +
            "FROM move_flags mf " +
            "JOIN move_flag_map mfm ON mfm.flag_id = mf.id " +
            "WHERE mfm.move_id = #{moveId}")
    List<Map<String, Object>> selectFlagsByMoveId(@Param("moveId") Integer moveId);
    
    /**
     * 批量获取多个技能的标记
     * 返回: Map<moveId, List<flagId>>
     */
    @Select("<script>" +
            "SELECT move_id, flag_id FROM move_flag_map WHERE move_id IN " +
            "<foreach item='id' collection='moveIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> selectFlagsByMoveIds(@Param("moveIds") List<Integer> moveIds);
}