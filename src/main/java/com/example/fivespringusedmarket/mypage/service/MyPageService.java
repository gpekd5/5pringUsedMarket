package com.example.fivespringusedmarket.mypage.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.mypage.dto.MyPageResponse;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
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
        // TODO: WishRepository가 안정화되면 관심 상품 count 쿼리로 교체한다.
        return 0L;
    }

    private Long getChatRoomCount(Long memberId) {
        // TODO: ChatRepository가 안정화되면 참여 채팅방 count 쿼리로 교체한다.
        return 0L;
    }

    private Long getCouponCount(Long memberId) {
        // TODO: CouponRepository가 안정화되면 보유 쿠폰 count 쿼리로 교체한다.
        return 0L;
    }
}
