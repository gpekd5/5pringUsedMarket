package com.example.fivespringusedmarket.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 상품 수정 요청 값이다. 전달된 필드만 수정되며 null인 필드는 변경되지 않는다.
 */
public record UpdateProductRequest(
        @Size(max = 100)
        String title,

        @Min(0)
        Integer price,

        String description,

        String category,

        List<String> images
) {
}
