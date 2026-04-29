package com.lio9.user.model;



import com.lio9.user.dto.UserProfile;

/**
 * 用户账号持久化模型。
 * <p>
 * 这个对象直接承接 MyBatis 从 app_user 表查询出来的结果，
 * 因此同时包含数据库内部字段（如 passwordHash）和可对外展示字段。
 * 对外返回前必须转换成 {@link UserProfile}，避免敏感字段泄漏。
 * </p>
 */
public class UserAccount {
    private Long id;
    private String username;
    private String displayName;
    private String passwordHash;
    private String createdAt;
    private String updatedAt;
    private String lastLoginAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(String lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * 转换成可安全返回给前端的用户资料对象。
     */
    public UserProfile toProfile() {
        return new UserProfile(id, username, displayName, createdAt, updatedAt, lastLoginAt);
    }
}
