package com.example.fivespringusedmarket.coupon.repository;

import com.example.fivespringusedmarket.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);
}
