package com.example.fivespringusedmarket.product.entity;

/**
 * 상품의 판매 상태를 나타내는 열거형이다.
 */
public enum ProductStatus {
    ON_SALE,    // 판매 중
    RESERVED,   // 예약 중
    SOLD,       // 판매 완료
    DELETE      // 삭제 처리
}
