package com.example.fivespringusedmarket.coupon.repository;

import com.example.fivespringusedmarket.coupon.entity.UserCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);

    Page<UserCoupon> findByMemberId(Long memberId, Pageable pageable);

    Page<UserCoupon> findByMemberIdAndUsedAtIsNull(Long memberId, Pageable pageable);

    Page<UserCoupon> findByMemberIdAndUsedAtIsNotNull(Long memberId, Pageable pageable);
}
