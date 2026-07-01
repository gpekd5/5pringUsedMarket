package com.example.fivespringusedmarket.coupon.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.coupon.dto.CouponResponse;
import com.example.fivespringusedmarket.coupon.dto.IssueCouponResponse;
import com.example.fivespringusedmarket.coupon.dto.UserCouponResponse;
import com.example.fivespringusedmarket.coupon.service.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 쿠폰 관련 API 를 처리하는 컨트롤러.
 * 이벤트 쿠폰 목록 조회, 선착순 발급, 내 쿠폰 조회, 쿠폰 사용을 담당한다.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

    private final LockService lockService;

    @GetMapping("/coupons")
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> getCoupons(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CouponResponse> result = lockService.getCoupons(pageable);
        return ResponseEntity.ok(ApiResponse.success("이벤트 쿠폰 목록 조회에 성공했습니다.", result));
    }

    @PostMapping("/coupons/{couponId}/issue")
    public ResponseEntity<ApiResponse<IssueCouponResponse>> issueCoupon(
            @PathVariable Long couponId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        // 동시성 제어를 위해 LockService 를 통해 발급
        IssueCouponResponse result = lockService.issueCouponWithLock(couponId, authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success("쿠폰 발급에 성공했습니다.", result));
    }

    @GetMapping("/members/me/coupons")
    public ResponseEntity<ApiResponse<Page<UserCouponResponse>>> getMyCoupons(
            @RequestParam(required = false) Boolean used,
            @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        // used 파라미터로 사용/미사용 필터링 가능
        Page<UserCouponResponse> result = lockService.getMyCoupons(authMember.memberId(), used, pageable);
        return ResponseEntity.ok(ApiResponse.success("내 쿠폰 목록 조회에 성공했습니다.", result));
    }

    @PatchMapping("/user-coupons/{userCouponId}/use")
    public ResponseEntity<ApiResponse<Void>> useCoupon(
            @PathVariable Long userCouponId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        lockService.useCoupon(userCouponId, authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success("쿠폰이 사용되었습니다.", null));
    }
}
