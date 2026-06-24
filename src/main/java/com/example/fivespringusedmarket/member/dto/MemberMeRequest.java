package com.example.fivespringusedmarket.member.dto;

import jakarta.validation.constraints.Size;

public record MemberMeRequest(
        @Size(max = 50)
        String nickname
) {
}
