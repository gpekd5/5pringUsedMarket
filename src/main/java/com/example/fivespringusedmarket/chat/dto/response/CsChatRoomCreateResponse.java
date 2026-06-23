package com.example.fivespringusedmarket.chat.dto.response;

import com.example.fivespringusedmarket.chat.entity.ChatRoom;

import java.time.LocalDateTime;

/**
 * CS 문의 채팅방 생성 응답 DTO
 */
public record CsChatRoomCreateResponse(
        Long roomId,
        String type,
        String title,
        String csStatus,
        LocalDateTime createdAt
) {
    public static CsChatRoomCreateResponse from(ChatRoom room) {
        return new CsChatRoomCreateResponse(
                room.getId(),
                room.getType().name(),
                room.getTitle(),
                room.getCsStatus().name(),
                room.getCreatedAt()
        );
    }
}
