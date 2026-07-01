package com.example.fivespringusedmarket.coupon.dto;

import com.example.fivespringusedmarket.coupon.entity.Coupon;
import java.time.LocalDateTime;

/**
 * 이벤트 쿠폰 목록 조회 응답 DTO.
 */
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
                // 잔여 수량 = 총 수량 - 발급된 수량
                coupon.getTotalQty() - coupon.getIssuedQty(),
                coupon.getEventStartAt(),
                coupon.getEventEndAt(),
                coupon.getExpireAt()
        );
    }
}
