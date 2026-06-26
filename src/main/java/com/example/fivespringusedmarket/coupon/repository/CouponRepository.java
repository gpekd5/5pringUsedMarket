package com.example.fivespringusedmarket.coupon.repository;

import com.example.fivespringusedmarket.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 이벤트 쿠폰 JPA 저장소.
 */
public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
