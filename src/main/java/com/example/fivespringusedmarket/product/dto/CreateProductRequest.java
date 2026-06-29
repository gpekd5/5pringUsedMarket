package com.example.fivespringusedmarket.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 상품 등록 요청 값이다.
 */
public record CreateProductRequest(
        @NotBlank
        @Size(max = 100)
        String title,

        @NotNull
        Integer price,

        String description,

        @NotBlank
        String category,

        List<String> imageKeys
) {
}
