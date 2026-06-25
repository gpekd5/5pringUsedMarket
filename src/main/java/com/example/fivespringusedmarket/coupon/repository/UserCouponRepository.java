package com.example.fivespringusedmarket.coupon.repository;

import com.example.fivespringusedmarket.coupon.entity.UserCoupon;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 발급 쿠폰 JPA 저장소.
 * 중복 발급 검사 및 사용/미사용 필터 조회를 제공한다.
 */
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    // 동일 회원이 동일 쿠폰을 이미 발급받았는지 확인
    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);

    // 본인 쿠폰인지 검증하면서 단건 조회
    Optional<UserCoupon> findByIdAndMemberId(Long id, Long memberId);

    Page<UserCoupon> findByMemberId(Long memberId, Pageable pageable);

    // usedAt 이 null 이면 미사용 쿠폰
    Page<UserCoupon> findByMemberIdAndUsedAtIsNull(Long memberId, Pageable pageable);

    // usedAt 이 null 이 아니면 사용 완료 쿠폰
    Page<UserCoupon> findByMemberIdAndUsedAtIsNotNull(Long memberId, Pageable pageable);
}
