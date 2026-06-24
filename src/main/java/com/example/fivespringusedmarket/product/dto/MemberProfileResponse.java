package com.example.fivespringusedmarket.product.dto;

import com.example.fivespringusedmarket.member.entity.Member;

/**
 * 회원 프로필 조회 응답 DTO다.
 */
public record MemberProfileResponse(
        Long memberId,
        String nickname,
        long productCount
) {
    public static MemberProfileResponse of(Member member, long productCount) {
        return new MemberProfileResponse(member.getId(), member.getNickname(), productCount);
    }
}
