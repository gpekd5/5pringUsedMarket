package com.example.fivespringusedmarket.product.dto;

import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductImage;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 등록 및 상세 조회 시 반환하는 응답 값이다.
 */
public record ProductResponse(
        Long id,
        Long memberId,
        String title,
        int price,
        String description,
        String category,
        String status,
        List<ProductImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 상품 이미지 정보를 담는 중첩 응답 값이다.
     */
    public record ProductImageResponse(
            Long id,
            String imageUrl,
            int sortOrder
    ) {
        public static ProductImageResponse from(ProductImage image) {
            return new ProductImageResponse(image.getId(), image.getImageUrl(), image.getSortOrder());
        }
    }

    public static ProductResponse of(Product product, List<ProductImage> images) {
        return new ProductResponse(
                product.getId(),
                product.getSeller().getId(),
                product.getTitle(),
                product.getPrice(),
                product.getDescription(),
                product.getCategory().name(),
                product.getStatus().name(),
                images.stream().map(ProductImageResponse::from).toList(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
