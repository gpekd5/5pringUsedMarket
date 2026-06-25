package com.example.fivespringusedmarket.coupon.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.coupon.dto.CouponResponse;
import com.example.fivespringusedmarket.coupon.dto.IssueCouponResponse;
import com.example.fivespringusedmarket.coupon.service.CouponService;
import com.example.fivespringusedmarket.coupon.service.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final LockService lockService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> getCoupons(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CouponResponse> result = couponService.getCoupons(pageable);
        return ResponseEntity.ok(ApiResponse.success("이벤트 쿠폰 목록 조회에 성공했습니다.", result));
    }

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<ApiResponse<IssueCouponResponse>> issueCoupon(
            @PathVariable Long couponId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        IssueCouponResponse result = lockService.issueCouponWithLock(couponId, authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success("쿠폰 발급에 성공했습니다.", result));
    }
}
