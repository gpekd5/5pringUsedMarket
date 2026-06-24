package com.example.fivespringusedmarket.coupon.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.coupon.dto.UserCouponResponse;
import com.example.fivespringusedmarket.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me/coupons")
@RequiredArgsConstructor
public class MemberCouponController {

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserCouponResponse>>> getMyCoupons(
            @RequestParam(required = false) Boolean used,
            @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        Page<UserCouponResponse> result = couponService.getMyCoupons(authMember.memberId(), used, pageable);
        return ResponseEntity.ok(ApiResponse.success("내 쿠폰 목록 조회에 성공했습니다.", result));
    }
}
