package com.example.fivespringusedmarket.wish.dto;

/**
 * 관심상품 등록/취소 상태 응답 DTO입니다.
 */
public record WishStatusResponse(
        Long productId,
        boolean wished
) {
    public static WishStatusResponse of(Long productId, boolean wished) {
        return new WishStatusResponse(productId, wished);
    }
}
