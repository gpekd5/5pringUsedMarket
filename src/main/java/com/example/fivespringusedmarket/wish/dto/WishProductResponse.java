package com.example.fivespringusedmarket.wish.dto;

import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductStatus;

import java.time.LocalDateTime;

/**
 * 내 관심상품 목록 조회 응답 DTO입니다.
 */
public record WishProductResponse(
        Long productId,
        String title,
        int price,
        ProductCategory category,
        ProductStatus status,
        String thumbnailUrl,
        LocalDateTime wishedAt
) {
}
