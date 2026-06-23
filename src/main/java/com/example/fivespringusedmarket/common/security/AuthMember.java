package com.example.fivespringusedmarket.common.security;

import com.example.fivespringusedmarket.member.entity.MemberRole;

/**
 * 인증된 회원 정보를 Controller에 전달하기 위한 Principal 객체다.
 */
public record AuthMember(
        Long memberId,
        String email,
        MemberRole role
) {
}
