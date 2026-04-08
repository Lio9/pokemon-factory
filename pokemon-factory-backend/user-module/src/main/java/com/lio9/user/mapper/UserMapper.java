package com.lio9.user.mapper;

import com.lio9.user.model.UserAccount;
import org.apache.ibatis.annotations.Param;

/**
 * 用户模块数据访问接口。
 * <p>
 * 这里故意保持接口层极简，把 SQL 细节全部放到 XML，
 * 以便和项目里其他 MyBatis XML 写法保持一致。
 * </p>
 */
public interface UserMapper {
    /**
     * 新建用户账号。
     */
    void insertUser(@Param("username") String username,
                    @Param("displayName") String displayName,
                    @Param("passwordHash") String passwordHash);

    /**
     * 按用户名查询账号信息。
     */
    UserAccount findByUsername(@Param("username") String username);

    /**
     * 登录成功后刷新登录时间与更新时间。
     */
    void touchLogin(@Param("id") Long id);
}
