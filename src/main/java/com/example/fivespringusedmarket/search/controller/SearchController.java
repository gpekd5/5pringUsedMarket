package com.example.fivespringusedmarket.search.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상품 검색 API를 제공하는 Controller입니다.
 */
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // 캐시가 적용되지 않은 QueryDsl 상품 검색 v1 API
    @GetMapping("/api/v1/products/search")
    public ResponseEntity<ApiResponse<ProductPageResponse>> searchProductV1(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 10) Pageable pageable
            ){
        ProductPageResponse response = searchService.searchProductsV1(keyword, category, status, sort, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
