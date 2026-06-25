package com.example.fivespringusedmarket.search.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.search.dto.RecentSearchResponse;
import com.example.fivespringusedmarket.search.service.SearchFacade;
import com.example.fivespringusedmarket.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상품 검색 API를 제공하는 Controller입니다.
 */
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchFacade searchFacade;

    // 캐시가 적용되지 않은 QueryDsl 상품 검색 v1 API
    @GetMapping("/api/v1/products/search")
    public ResponseEntity<ApiResponse<ProductPageResponse>> searchProductV1(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 10) Pageable pageable
            ){
        Long memberId = authMember == null ? null : authMember.memberId();

        ProductPageResponse response = searchFacade.searchProductsV1(memberId, keyword, category, status, sort, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @GetMapping("/api/search/recent")
    public ResponseEntity<ApiResponse<List<RecentSearchResponse>>> getRecentSearches(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        List<RecentSearchResponse> response = searchFacade.getRecentSearches(authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/api/search/recent/{searchLogId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecentSearch(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long searchLogId
    ) {
        searchFacade.deleteRecentSearch(authMember.memberId(), searchLogId);
        return ResponseEntity.ok(ApiResponse.success("기록이 삭제되었습니다.", null));
    }



}
