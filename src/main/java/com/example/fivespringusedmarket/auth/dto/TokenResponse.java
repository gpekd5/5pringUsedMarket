package com.example.fivespringusedmarket.auth.dto;

/**
 * 로그인과 토큰 재발급 성공 후 반환하는 토큰 응답 값이다.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    public static TokenResponse from(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken, BEARER_TOKEN_TYPE);
    }
}
