package com.example.fivespringusedmarket.search.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.product.dto.ProductListItemResponse;
import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.search.dto.ProductSearchCondition;
import com.example.fivespringusedmarket.search.dto.ProductSearchSortType;
import com.example.fivespringusedmarket.search.dto.RecentSearchResponse;
import com.example.fivespringusedmarket.search.entity.SearchLog;
import com.example.fivespringusedmarket.search.repository.ProductSearchRepository;
import com.example.fivespringusedmarket.search.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 상품 검색 비즈니스 로직을 처리하는 Service입니다.
 *
 * <p>검색 조건과 페이징 정보를 Repository에 전달하고,
 * 조회 결과를 상품 목록 페이지 응답 형태로 변환합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final ProductSearchRepository productSearchRepository;
    private final SearchLogRepository searchLogRepository;

    // 캐시가 적용되지 않은 QueryDsl 상품 검색 v1 기능
    @Transactional
    public ProductPageResponse searchProductsV1(Member member,String keyword, String category, String status, String sort, Pageable pageable)
    {
        ProductSearchCondition condition = new ProductSearchCondition(
                keyword,
                parseCategory(category),
                parseStatus(status),
                parseSort(sort)
        );

        saveSearchLog(member, keyword);

        Page<ProductListItemResponse> products = productSearchRepository.search(condition, pageable);

        return ProductPageResponse.of(products);
    }

    public List<RecentSearchResponse> getRecentSearches(Long memberId) {
        return searchLogRepository.findTop10ByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(RecentSearchResponse::from)
                .toList();
    }

    private void saveSearchLog(Member member, String keyword) {
        if (member == null || !StringUtils.hasText(keyword)) {
            return;
        }

        searchLogRepository.save(SearchLog.create(member, keyword.trim())); // 앞뒤 공백 제거하기 위해 트림 사용
    }

    private ProductCategory parseCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }

        try{
            return ProductCategory.valueOf(category.toUpperCase());
        }catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_CATEGORY);
        }
    }

    private ProductStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return ProductStatus.ON_SALE; // 일반 사용자가 상품 검색하면 보통 판매중 상품만 기본 노출
        }

        try{
            return ProductStatus.valueOf(status.toUpperCase());
        }catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_STATUS);
        }
    }

    private ProductSearchSortType parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return ProductSearchSortType.LATEST;
        }

        try{
            return ProductSearchSortType.valueOf(sort.toUpperCase());
        }catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_SORT_TYPE);
        }
    }
}
