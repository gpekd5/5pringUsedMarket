package com.example.fivespringusedmarket.product.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 상품 목록 페이지네이션 응답 값이다.
 */
public record ProductPageResponse(
        List<ProductListItemResponse> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {

    public static ProductPageResponse of(Page<ProductListItemResponse> page) {
        return new ProductPageResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber()
        );
    }
}
