package com.example.fivespringusedmarket.product.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 상품 상태 변경 요청 DTO다.
 */
public record UpdateProductStatusRequest(
        @NotBlank String status
) {
}
