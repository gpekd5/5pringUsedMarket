package com.example.fivespringusedmarket.coupon.dto;

import com.example.fivespringusedmarket.coupon.entity.Coupon;
import java.time.LocalDateTime;

public record CouponResponse(
        Long couponId,
        String name,
        int totalQty,
        int remainQty,
        LocalDateTime eventStartAt,
        LocalDateTime eventEndAt,
        LocalDateTime expireAt
) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getTotalQty(),
                coupon.getTotalQty() - coupon.getIssuedQty(),
                coupon.getEventStartAt(),
                coupon.getEventEndAt(),
                coupon.getExpireAt()
        );
    }
}
