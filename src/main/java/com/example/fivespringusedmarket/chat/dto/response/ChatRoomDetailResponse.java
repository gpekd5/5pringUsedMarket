package com.example.fivespringusedmarket.chat.dto.response;

import com.example.fivespringusedmarket.chat.entity.ChatMember;
import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import com.example.fivespringusedmarket.chat.entity.ChatRoomType;
import com.example.fivespringusedmarket.member.entity.Member;

import java.time.LocalDateTime;

/**
 * 채팅방 상세 조회 응답 DTO다.
 * TRADE, CS 유형을 하나의 DTO로 처리하며 유형별 필드는 null이 될 수 있다.
 */
public record ChatRoomDetailResponse(
        Long roomId,
        String type,
        // CS 전용. TRADE면 null.
        String title,
        // CS 전용. TRADE면 null.
        String csStatus,
        // TRADE 전용. CS면 null.
        ProductDetail product,
        CounterpartInfo counterpart,
        LocalDateTime createdAt
) {
    /*
     채팅 상대방 정보 DTO다.
     상세 조회 응답에서 profileImage까지 포함한다.
    */
    public record CounterpartInfo(
            Long memberId,
            String nickname
    ) {
        public static CounterpartInfo from(Member member) {
            return new CounterpartInfo(member.getId(), member.getNickname());
        }
    }

    public record ProductDetail(Long id, String title, int price, String status) {
        public static ProductDetail from(com.example.fivespringusedmarket.product.entity.Product product) {
            return new ProductDetail(
                    product.getId(),
                    product.getTitle(),
                    product.getPrice(),
                    product.getStatus().name()
            );
        }
    }

    public static ChatRoomDetailResponse of(ChatRoom room, ChatMember counterpartMember) {
        CounterpartInfo counterpart = counterpartMember != null
                ? CounterpartInfo.from(counterpartMember.getMember())
                : null;

        ProductDetail productDetail = room.getType() == ChatRoomType.TRADE && room.getProduct() != null
                ? ProductDetail.from(room.getProduct())
                : null;

        String csStatus = room.getType() == ChatRoomType.CS && room.getCsStatus() != null
                ? room.getCsStatus().name()
                : null;

        return new ChatRoomDetailResponse(
                room.getId(),
                room.getType().name(),
                room.getTitle(),
                csStatus,
                productDetail,
                counterpart,
                room.getCreatedAt()
        );
    }
}
