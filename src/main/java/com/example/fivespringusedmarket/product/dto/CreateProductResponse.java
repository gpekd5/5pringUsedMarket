package com.example.fivespringusedmarket.product.dto;

import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductImage;
import java.time.LocalDateTime;
import java.util.List;

public record CreateProductResponse(
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

    public record ProductImageResponse(
            Long id,
            String imageUrl,
            int sortOrder
    ) {
        public static ProductImageResponse from(ProductImage image) {
            return new ProductImageResponse(image.getId(), image.getImageUrl(), image.getSortOrder());
        }
    }

    public static CreateProductResponse of(Product product, List<ProductImage> images) {
        return new CreateProductResponse(
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
