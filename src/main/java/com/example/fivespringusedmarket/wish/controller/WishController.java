package com.example.fivespringusedmarket.wish.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.search.dto.RecentSearchResponse;
import com.example.fivespringusedmarket.wish.dto.WishProductResponse;
import com.example.fivespringusedmarket.wish.dto.WishStatusResponse;
import com.example.fivespringusedmarket.wish.service.WishFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관심상품 API를 제공하는 Controller입니다.
 */
@RestController
@RequiredArgsConstructor
public class WishController {

    private final WishFacade wishFacade;

    @PostMapping("/api/products/{productId}/wishes")
    public ResponseEntity<ApiResponse<WishStatusResponse>> addWish(@AuthenticationPrincipal AuthMember authMember, @PathVariable Long productId) {
        WishStatusResponse response = wishFacade.addWish(authMember.memberId(), productId);
        return ResponseEntity.ok(ApiResponse.success("관심상품으로 등록되었습니다.", response));
    }

    @DeleteMapping("/api/products/{productId}/wishes")
    public ResponseEntity<ApiResponse<WishStatusResponse>> removeWish(@AuthenticationPrincipal AuthMember authMember, @PathVariable Long productId) {
        WishStatusResponse response = wishFacade.removeWish(authMember.memberId(), productId);
        return ResponseEntity.ok(ApiResponse.success("관심상품이 취소되었습니다.",response));
    }

    @GetMapping("/api/members/me/wishes")
    public ApiResponse<List<WishProductResponse>> getMyWishes(
            @AuthenticationPrincipal AuthMember authUser
    ) {
        List<WishProductResponse> response = wishFacade.getMyWishes(authUser.memberId());

        return ApiResponse.success("관심상품 목록 조회에 성공했습니다.", response);
    }
}
