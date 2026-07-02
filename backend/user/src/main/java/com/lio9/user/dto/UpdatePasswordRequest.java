package com.lio9.user.dto;

/** 更改密码请求 */
public class UpdatePasswordRequest {
    private String currentPassword;
    private String newPassword;

    public UpdatePasswordRequest() {}
    public UpdatePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String currentPassword() { return currentPassword; }
    public String newPassword() { return newPassword; }

    public void setCurrentPassword(String v) { this.currentPassword = v; }
    public void setNewPassword(String v) { this.newPassword = v; }
}
