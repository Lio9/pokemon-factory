package com.lio9.user.dto;

/** 邮箱验证确认请求 */
public class VerifyEmailRequest {
    private String token;

    public VerifyEmailRequest() {}
    public VerifyEmailRequest(String token) { this.token = token; }

    public String token() { return token; }
    public void setToken(String v) { this.token = v; }
}
