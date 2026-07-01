package com.example.fivespringusedmarket.mypage.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.coupon.repository.UserCouponRepository;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.mypage.dto.MyPageResponse;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import com.example.fivespringusedmarket.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마이페이지 첫 화면에 필요한 회원 요약 정보를 조회한다.
 */
@Service
@RequiredArgsConstructor
public class MyPageService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final WishRepository wishRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 인증된 회원의 마이페이지 요약 정보를 반환한다.
     *
     * @param memberId 인증 Principal에서 추출한 회원 ID
     * @return 회원 기본 정보와 도메인별 요약 카운트
     */
    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return new MyPageResponse(
                member.getId(),
                member.getNickname(),
                getSellingProductCount(memberId),
                getWishedProductCount(memberId),
                getChatRoomCount(memberId),
                getCouponCount(memberId)
        );
    }

    private Long getSellingProductCount(Long memberId) {
        return productRepository.countBySellerIdAndStatusNot(memberId, ProductStatus.DELETED);
    }

    private Long getWishedProductCount(Long memberId) {
        return wishRepository.countByMemberIdAndProductStatusNot(memberId, ProductStatus.DELETED);
    }

    private Long getChatRoomCount(Long memberId) {
        return chatMemberRepository.countByMemberId(memberId);
    }

    private Long getCouponCount(Long memberId) {
        return userCouponRepository.countByMemberId(memberId);
    }
}
