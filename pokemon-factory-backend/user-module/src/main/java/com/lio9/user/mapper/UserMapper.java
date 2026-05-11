package com.lio9.user.mapper;

import com.lio9.user.model.UserAccount;
import org.apache.ibatis.annotations.Param;

/** 用户模块数据访问接口，SQL 全部集中在 XML 中维护 */
public interface UserMapper {

    void insertUser(@Param("username") String username,
                    @Param("displayName") String displayName,
                    @Param("passwordHash") String passwordHash);

    UserAccount findByUsername(@Param("username") String username);

    void touchLogin(@Param("id") Long id);

    /** 更新用户昵称 */
    void updateDisplayName(@Param("id") Long id, @Param("displayName") String displayName);

    /** 更新密码 + 递增 token 版本（使旧令牌失效） */
    void updatePassword(@Param("id") Long id,
                        @Param("passwordHash") String passwordHash);

    /** 递增 token 版本（使该用户所有令牌失效） */
    void incrementTokenVersion(@Param("id") Long id);

    /** 重置失败尝试次数 */
    void resetFailedAttempts(@Param("id") Long id);

    /** 记录失败尝试 + 超过阈值时锁定 */
    void incrementFailedAttempts(@Param("id") Long id);

    /** 解锁账号 */
    void unlockAccount(@Param("id") Long id);
}
