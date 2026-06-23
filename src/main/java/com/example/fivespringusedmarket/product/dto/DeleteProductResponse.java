package com.example.fivespringusedmarket.product.dto;

/**
 * 상품 삭제 성공 시 반환하는 응답 값이다.
 */
public record DeleteProductResponse(
        String message,
        Long productId
) {
}
