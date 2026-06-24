package com.example.fivespringusedmarket.search.dto;

import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductStatus;

/**
 * 상품 검색 조건을 담는 DTO입니다.
 *
 * <p>키워드, 카테고리, 판매 상태, 정렬 기준을 조합하여
 * 상품 검색 쿼리를 생성할 때 사용합니다.</p>
 *
 * @param keyword 상품명 또는 상품 설명 검색 키워드
 * @param category 상품 카테고리
 * @param status 상품 판매 상태
 * @param sort 검색 결과 정렬 기준
 */
public record ProductSearchCondition(
        String keyword,
        ProductCategory category,
        ProductStatus status,
        ProductSearchSortType sort
) {
}
