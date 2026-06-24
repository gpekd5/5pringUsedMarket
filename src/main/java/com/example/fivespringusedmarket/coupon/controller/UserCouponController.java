package com.example.fivespringusedmarket.coupon.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-coupons")
@RequiredArgsConstructor
public class UserCouponController {

    private final CouponService couponService;

    @PatchMapping("/{userCouponId}/use")
    public ResponseEntity<ApiResponse<Void>> useCoupon(
            @PathVariable Long userCouponId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        couponService.useCoupon(userCouponId, authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success("쿠폰이 사용되었습니다.", null));
    }
}
