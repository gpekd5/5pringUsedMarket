package com.example.fivespringusedmarket.product.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 상품 목록 페이지네이션 응답 값이다.
 */
public record ProductPageResponse(
        List<ProductListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ProductPageResponse of(Page<ProductListItemResponse> page) {
        return new ProductPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
