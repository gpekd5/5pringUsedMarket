package com.example.fivespringusedmarket.chat.dto.response;

import com.example.fivespringusedmarket.chat.entity.ChatMember;
import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import com.example.fivespringusedmarket.chat.entity.ChatRoomType;
import com.example.fivespringusedmarket.product.entity.Product;

import java.time.LocalDateTime;

/**
  채팅방 목록 조회 응답 DTO
  TRADE, CS 유형을 하나의 DTO로 처리하며 유형별 필드는 null이 될 수 있다
 */
public record ChatRoomListResponse(
        Long roomId,
        String type,
        String title,
        String thumbnailUrl,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount,
        CounterpartSummary counterpart,
        // TRADE 전용. CS면 null.
        ProductSummary product,
        // CS 전용. TRADE면 null.
        String csStatus
) {
    public record ProductSummary(Long id, String status) {
        public static ProductSummary from(Product product) {
            return new ProductSummary(product.getId(), product.getStatus().name());
        }
    }

    public static ChatRoomListResponse of(ChatRoom room, ChatMember counterpartMember, long unreadCount) {
        CounterpartSummary counterpart = counterpartMember != null
                ? CounterpartSummary.from(counterpartMember.getMember())
                : null;

        ProductSummary productSummary = room.getType() == ChatRoomType.TRADE && room.getProduct() != null
                ? ProductSummary.from(room.getProduct())
                : null;

        String thumbnailUrl = room.getType() == ChatRoomType.TRADE && room.getProduct() != null
                ? room.getProduct().getThumbnailUrl()
                : null;

        String csStatus = room.getType() == ChatRoomType.CS && room.getCsStatus() != null
                ? room.getCsStatus().name()
                : null;

        return new ChatRoomListResponse(
                room.getId(),
                room.getType().name(),
                room.getTitle(),
                thumbnailUrl,
                room.getLastMessageContent(),
                room.getLastMessageAt(),
                unreadCount,
                counterpart,
                productSummary,
                csStatus
        );
    }
}
