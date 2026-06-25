package com.example.fivespringusedmarket.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Access Token 재발급에 사용하는 Refresh Token 요청 값이다.
 */
public record ReissueRequest(
        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {
}
