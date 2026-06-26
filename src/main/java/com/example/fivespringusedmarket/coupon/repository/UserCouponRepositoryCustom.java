package com.example.fivespringusedmarket.coupon.repository;

import com.example.fivespringusedmarket.coupon.entity.UserCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserCouponRepositoryCustom {

    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);

    Optional<UserCoupon> findByIdAndMemberId(Long id, Long memberId);

    Page<UserCoupon> findByMemberId(Long memberId, Pageable pageable);

    Page<UserCoupon> findByMemberIdAndUsedAtIsNull(Long memberId, Pageable pageable);

    Page<UserCoupon> findByMemberIdAndUsedAtIsNotNull(Long memberId, Pageable pageable);
}
