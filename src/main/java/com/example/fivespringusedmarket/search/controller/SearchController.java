package com.example.fivespringusedmarket.search.controller;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import com.example.fivespringusedmarket.search.dto.PopularSearchResponse;
import com.example.fivespringusedmarket.search.dto.RecentSearchResponse;
import com.example.fivespringusedmarket.search.service.SearchFacade;
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
    private static final int MAX_SEARCH_PAGE_SIZE = 50;

    /**
     * 캐시가 적용되지 않은 상품 검색 v1 API입니다.
     *
     * <p>비로그인 사용자도 검색할 수 있으므로 authMember는 null일 수 있습니다.
     * 로그인 사용자인 경우에만 memberId를 전달하여 검색어 저장 및 인기검색어 집계를 수행합니다.</p>
     */
    @GetMapping("/api/v1/products/search")
    public ResponseEntity<ApiResponse<ProductPageResponse>> searchProductV1(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 10) Pageable pageable
            ){
        validateSearchPageSize(pageable);

        Long memberId = authMember == null ? null : authMember.memberId();

        ProductPageResponse response = searchFacade.searchProductsV1(memberId, keyword, category, status, sort, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Caffeine 캐시가 적용된 않은 상품 검색 v2 API입니다.
     *
     * <p>비로그인 사용자도 검색할 수 있으므로 authMember는 null일 수 있습니다.
     * 로그인 사용자인 경우에만 memberId를 전달하여 검색어 저장 및 인기검색어 집계를 수행합니다.</p>
     */
    @GetMapping("/api/v2/products/search")
    public ResponseEntity<ApiResponse<ProductPageResponse>> searchProductV2(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 10) Pageable pageable
    ){
        validateSearchPageSize(pageable);

        Long memberId = authMember == null ? null : authMember.memberId();

        ProductPageResponse response = searchFacade.searchProductsV2(memberId, keyword, category, status, sort, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    /**
     * 로그인 사용자의 최근 검색어 목록을 조회합니다.
     *
     * <p>최근 검색어는 사용자별 데이터이므로 인증 사용자 정보가 필요합니다.
     * memberId는 요청 값으로 받지 않고 인증 Principal에서 가져옵니다.</p>
     */
    @GetMapping("/api/search/recent")
    public ResponseEntity<ApiResponse<List<RecentSearchResponse>>> getRecentSearches(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        List<RecentSearchResponse> response = searchFacade.getRecentSearches(authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 로그인 사용자의 최근 검색어를 삭제합니다.
     *
     * <p>삭제 권한 검증은 Service 계층에서 인증 사용자 ID와 검색 기록의 memberId를 비교하여 처리합니다.</p>
     */
    @DeleteMapping("/api/search/recent/{searchLogId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecentSearch(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long searchLogId
    ) {
        searchFacade.deleteRecentSearch(authMember.memberId(), searchLogId);
        return ResponseEntity.ok(ApiResponse.success("기록이 삭제되었습니다.", null));
    }

    /**
     * 인기검색어 Top 10 목록을 조회합니다.
     *
     * <p>인기검색어는 전체 사용자에게 노출되는 공개 데이터이므로 인증이 필요하지 않습니다.
     * 단, 집계 기준은 로그인 사용자의 keyword 검색 기록입니다.</p>
     */
    @GetMapping("/api/search/popular")
    public ResponseEntity<ApiResponse<List<PopularSearchResponse>>> getPopularSearches() {
        List<PopularSearchResponse> responses = searchFacade.getPopularSearches();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    private void validateSearchPageSize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_SEARCH_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }



}
