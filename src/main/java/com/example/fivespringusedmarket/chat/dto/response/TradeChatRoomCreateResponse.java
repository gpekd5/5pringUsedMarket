package com.example.fivespringusedmarket.chat.dto.response;

import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.product.entity.Product;

import java.time.LocalDateTime;

public record TradeChatRoomCreateResponse(
        Long roomId,
        String type,
        ProductDetail product,
        CounterpartSummary counterpart,
        LocalDateTime createdAt
) {
    public record ProductDetail(Long id, String title, int price) {
        public static ProductDetail from(Product product) {
            return new ProductDetail(
                    product.getId(),
                    product.getTitle(),
                    product.getPrice()
            );
        }
    }

    public static TradeChatRoomCreateResponse of(ChatRoom room, Product product, Member counterpart) {
        return new TradeChatRoomCreateResponse(
                room.getId(),
                room.getType().name(),
                ProductDetail.from(product),
                CounterpartSummary.from(counterpart),
                room.getCreatedAt()
        );
    }
}