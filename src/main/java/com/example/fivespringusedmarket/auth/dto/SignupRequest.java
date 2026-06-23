package com.example.fivespringusedmarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 값이다.
 */
public record SignupRequest(
        @NotBlank
        @Email
        @Size(max = 100)
        String email,

        @NotBlank
        @Size(max = 255)
        String password,

        @NotBlank
        @Size(max = 50)
        String nickname
) {
}
