package com.example.fivespringusedmarket.coupon.dto;

import com.example.fivespringusedmarket.coupon.entity.UserCoupon;
import java.time.LocalDateTime;

/**
 * 내 쿠폰 목록 조회 응답 DTO.
 */
public record UserCouponResponse(
        Long userCouponId,
        Long couponId,
        String couponName,
        String code,
        LocalDateTime issuedAt,
        LocalDateTime expireAt,
        LocalDateTime usedAt
) {

    public static UserCouponResponse from(UserCoupon userCoupon) {
        return new UserCouponResponse(
                userCoupon.getId(),
                userCoupon.getCoupon().getId(),
                userCoupon.getCoupon().getName(),
                userCoupon.getCode(),
                userCoupon.getIssuedAt(),
                userCoupon.getExpireAt(),
                userCoupon.getUsedAt()
        );
    }
}
