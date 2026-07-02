package com.lio9.user.dto;

/** 邮箱注册请求 */
public class EmailVerificationRequest {
    private String email;

    public EmailVerificationRequest() {}
    public EmailVerificationRequest(String email) { this.email = email; }

    public String email() { return email; }
    public void setEmail(String v) { this.email = v; }
}
