package com.example.fivespringusedmarket.coupon.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.coupon.dto.CouponResponse;
import com.example.fivespringusedmarket.coupon.dto.IssueCouponResponse;
import com.example.fivespringusedmarket.coupon.dto.UserCouponResponse;
import com.example.fivespringusedmarket.coupon.entity.Coupon;
import com.example.fivespringusedmarket.coupon.entity.UserCoupon;
import com.example.fivespringusedmarket.coupon.repository.CouponRepository;
import com.example.fivespringusedmarket.coupon.repository.UserCouponRepository;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쿠폰 비즈니스 로직을 담당하는 서비스.
 * 발급, 조회, 사용 처리를 수행하며 LockService 를 통해 호출되는 issueCoupon 은 Redis Lock 보호 하에 실행된다.
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<CouponResponse> getCoupons(Pageable pageable) {
        return couponRepository.findAll(pageable).map(CouponResponse::from);
    }

    @Transactional
    public IssueCouponResponse issueCoupon(Long couponId, Long memberId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        // 이벤트 기간, 중복 발급, 재고 순으로 검증
        if (coupon.isEventNotStarted(now)) {
            throw new CustomException(ErrorCode.COUPON_EVENT_NOT_STARTED);
        }
        if (coupon.isEventEnded(now)) {
            throw new CustomException(ErrorCode.COUPON_EVENT_ENDED);
        }
        if (productRepository.countBySellerIdAndStatusNot(memberId, ProductStatus.DELETED) == 0) {
            throw new CustomException(ErrorCode.COUPON_ISSUE_REQUIRES_PRODUCT);
        }
        if (userCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
        if (!coupon.hasStock()) {
            throw new CustomException(ErrorCode.COUPON_OUT_OF_STOCK);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 수량 차감과 UserCoupon 저장을 하나의 트랜잭션에서 처리
        coupon.incrementIssuedQty();
        String code = "COUPON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        UserCoupon userCoupon = UserCoupon.issue(member, coupon, code);
        userCouponRepository.save(userCoupon);

        return IssueCouponResponse.from(userCoupon);
    }

    @Transactional(readOnly = true)
    public Page<UserCouponResponse> getMyCoupons(Long memberId, Boolean used, Pageable pageable) {
        // used 파라미터가 없으면 전체, true 면 사용 완료, false 면 미사용 목록 반환
        if (used == null) {
            return userCouponRepository.findByMemberId(memberId, pageable).map(UserCouponResponse::from);
        }
        if (used) {
            return userCouponRepository.findByMemberIdAndUsedAtIsNotNull(memberId, pageable).map(UserCouponResponse::from);
        }
        return userCouponRepository.findByMemberIdAndUsedAtIsNull(memberId, pageable).map(UserCouponResponse::from);
    }

    @Transactional
    public void useCoupon(Long userCouponId, Long memberId) {
        // 본인 쿠폰인지 확인하면서 조회
        UserCoupon userCoupon = userCouponRepository.findByIdAndMemberId(userCouponId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_COUPON_NOT_FOUND));

        if (userCoupon.isUsed()) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_USED);
        }
        if (userCoupon.isExpired()) {
            throw new CustomException(ErrorCode.COUPON_EXPIRED);
        }

        userCoupon.use();
    }
}
