package com.lio9.user.dto;

/** 认证请求体 — 登录和注册复用同一结构 */
public class AuthRequest {
    private String username;
    private String password;

    public AuthRequest() {}
    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String username() { return username; }
    public String password() { return password; }

    public void setUsername(String v) { this.username = v; }
    public void setPassword(String v) { this.password = v; }
}
