package com.example.fivespringusedmarket.product.dto;

import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductImage;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 등록, 수정, 상세 조회 시 반환하는 응답 값이다.
 */
public record ProductResponse(
        Long productId,
        Long sellerId,
        String sellerNickname,
        String title,
        int price,
        String description,
        String category,
        String status,
        List<String> imageUrls,
        boolean wished,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductResponse of(Product product, List<ProductImage> images) {
        return new ProductResponse(
                product.getId(),
                product.getSeller().getId(),
                product.getSeller().getNickname(),
                product.getTitle(),
                product.getPrice(),
                product.getDescription(),
                product.getCategory().name(),
                product.getStatus().name(),
                images.stream().map(ProductImage::getImageUrl).toList(),
                false,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
