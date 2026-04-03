package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlayerMapper {
    @Insert("INSERT OR IGNORE INTO player(username, rank, points) VALUES(#{username}, 0, 0)")
    void insertIgnore(@Param("username") String username);

    @Select("SELECT id FROM player WHERE username = #{username}")
    Integer findIdByUsername(@Param("username") String username);
}
