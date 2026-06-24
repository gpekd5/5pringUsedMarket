package com.example.fivespringusedmarket.auth.dto;

/**
 * 로그인 성공 후 발급되는 토큰 응답 값이다.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    public static LoginResponse from(String accessToken, String refreshToken) {
        return new LoginResponse(accessToken, refreshToken, BEARER_TOKEN_TYPE);
    }
}
