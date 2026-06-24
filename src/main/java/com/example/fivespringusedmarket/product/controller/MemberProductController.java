package com.example.fivespringusedmarket.product.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.product.dto.MemberProfileResponse;
import com.example.fivespringusedmarket.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 관련 상품 조회 HTTP 요청을 처리하는 Controller다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberProductController {

    private final ProductService productService;

    @GetMapping("/{memberId}/profile")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMemberProfile(
            @PathVariable Long memberId
    ) {
        MemberProfileResponse response = productService.getMemberProfile(memberId);
        return ResponseEntity.ok(ApiResponse.success("판매자 프로필 조회에 성공했습니다.", response));
    }
}
