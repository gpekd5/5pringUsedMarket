package com.example.fivespringusedmarket.mypage.dto;

/**
 * 마이페이지 첫 화면에 표시할 회원 요약 응답이다.
 */
public record MyPageResponse(
        Long memberId,
        String nickname,
        long sellingProductCount,
        long wishedProductCount,
        long chatRoomCount,
        long couponCount
) {
}
