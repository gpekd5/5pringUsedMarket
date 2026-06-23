package com.example.fivespringusedmarket.auth.dto;

/**
 * 로그인 성공 후 발급되는 Access Token 응답 값이다.
 */
public record LoginResponse(
        String accessToken,
        String tokenType
) {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    public static LoginResponse from(String accessToken) {
        // 이번 PR에서는 Refresh Token 없이 Access Token만 응답한다.
        return new LoginResponse(accessToken, BEARER_TOKEN_TYPE);
    }
}
