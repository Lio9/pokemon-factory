package com.lio9.user.dto;

/** 更新用户资料请求 */
public class UpdateProfileRequest {
    private String displayName;

    public UpdateProfileRequest() {}
    public UpdateProfileRequest(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }
}
