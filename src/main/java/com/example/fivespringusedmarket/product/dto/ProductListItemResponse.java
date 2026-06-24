package com.example.fivespringusedmarket.product.dto;

import com.example.fivespringusedmarket.product.entity.Product;
import java.time.LocalDateTime;

/**
 * 상품 목록 조회 시 각 상품 항목을 나타내는 응답 값이다.
 */
public record ProductListItemResponse(
        Long productId,
        Long sellerId,
        String title,
        int price,
        String category,
        String status,
        String thumbnailUrl,
        LocalDateTime createdAt
) {

    public static ProductListItemResponse of(Product product, String thumbnailUrl) {
        return new ProductListItemResponse(
                product.getId(),
                product.getSeller().getId(),
                product.getTitle(),
                product.getPrice(),
                product.getCategory().name(),
                product.getStatus().name(),
                thumbnailUrl,
                product.getCreatedAt()
        );
    }
}
