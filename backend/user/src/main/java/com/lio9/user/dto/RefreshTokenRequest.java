package com.lio9.user.dto;

/** 刷新令牌请求 */
public class RefreshTokenRequest {
    private String refreshToken;

    public RefreshTokenRequest() {}
    public RefreshTokenRequest(String refreshToken) { this.refreshToken = refreshToken; }

    public String refreshToken() { return refreshToken; }
    public void setRefreshToken(String v) { this.refreshToken = v; }
}
