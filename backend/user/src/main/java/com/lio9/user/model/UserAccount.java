package com.lio9.user.model;

import com.lio9.user.dto.UserProfile;

/**
 * 用户账号持久化模型。
 *
 * <p>直接承接 MyBatis 从 {@code app_user} 表的查询结果。
 * 对外返回前必须调用 {@link #toProfile()} 转换为 {@link UserProfile}，
 * 避免 {@code passwordHash} 等敏感字段泄漏。</p>
 */
public class UserAccount {
    private Long id;
    private String username;
    private String displayName;
    private String passwordHash;
    private String email;
    private Integer emailVerified;
    private String verificationToken;
    private Integer tokenVersion;
    private Integer failedAttempts;
    private String lockedUntil;
    private String createdAt;
    private String updatedAt;
    private String lastLoginAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Integer emailVerified) { this.emailVerified = emailVerified; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public Integer getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(Integer tokenVersion) { this.tokenVersion = tokenVersion; }

    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }

    public String getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(String lockedUntil) { this.lockedUntil = lockedUntil; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(String lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    /** 转换成可安全返回给前端的用户资料对象 */
    public UserProfile toProfile() {
        Boolean emailVerified = null;
        if (this.email != null) {
            emailVerified = this.emailVerified != null && this.emailVerified == 1;
        }
        return new UserProfile(id, username, displayName, email, emailVerified, createdAt, updatedAt, lastLoginAt);
    }
}
