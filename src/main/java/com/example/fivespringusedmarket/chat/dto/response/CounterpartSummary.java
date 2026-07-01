package com.example.fivespringusedmarket.chat.dto.response;

import com.example.fivespringusedmarket.member.entity.Member;

/**
  채팅 상대방 요약 정보 DTO
  목록/생성 응답처럼 profileImage가 필요 없는 경우에 사용
 */
public record CounterpartSummary(
        Long memberId,
        String nickname
) {
    public static CounterpartSummary from(Member member) {
        return new CounterpartSummary(member.getId(), member.getNickname());
    }
}
