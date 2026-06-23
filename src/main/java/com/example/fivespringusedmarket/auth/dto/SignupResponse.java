package com.example.fivespringusedmarket.auth.dto;

/**
 * 회원가입 성공 응답 값이다.
 */
public record SignupResponse(
        Long memberId,
        String email,
        String nickname
) {
}
