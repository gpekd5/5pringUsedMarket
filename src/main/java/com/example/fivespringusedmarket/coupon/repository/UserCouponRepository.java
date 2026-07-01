package com.example.fivespringusedmarket.coupon.repository;

import com.example.fivespringusedmarket.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;


/**
 * 회원 발급 쿠폰 JPA 저장소.
 * 중복 발급 검사 및 사용/미사용 필터 조회는 UserCouponRepositoryCustom(QueryDSL)으로 처리한다.
 */
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long>, UserCouponRepositoryCustom {

    long countByMemberId(Long memberId);
}
