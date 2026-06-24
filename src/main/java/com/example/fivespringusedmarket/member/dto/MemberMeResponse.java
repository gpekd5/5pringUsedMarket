package com.example.fivespringusedmarket.member.dto;

public record MemberMeResponse(
        Long memberId,
        String email,
        String nickname
) {
}
