package com.example.fivespringusedmarket.coupon.dto;

import com.example.fivespringusedmarket.coupon.entity.UserCoupon;
import java.time.LocalDateTime;

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
