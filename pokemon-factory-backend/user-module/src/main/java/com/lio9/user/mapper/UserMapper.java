package com.lio9.user.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO app_user(username, password_hash, created_at) VALUES(#{username}, #{passwordHash}, datetime('now'))")
    void insertUser(@Param("username") String username, @Param("passwordHash") String passwordHash);

    @Select("SELECT id, username, password_hash AS passwordHash FROM app_user WHERE username = #{username}")
    Map<String,Object> findByUsername(@Param("username") String username);
}
