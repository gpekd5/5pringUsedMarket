package com.example.fivespringusedmarket.chat.dto.response;

import com.example.fivespringusedmarket.chat.entity.ChatMessage;

import java.time.LocalDateTime;

/**
 STOMP 채팅방 구독자에게 브로드캐스트하는 메시지 DTO다.
 /sub/chat/{roomId} 로 전송된다.
 */
public record ChatMessageBroadcast(
        Long messageId,
        Long roomId,
        String type,
        Long senderId,
        String senderNickname,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageBroadcast from(ChatMessage message) {
        Long senderId = message.getSender() != null ? message.getSender().getId() : null;
        String senderNickname = message.getSender() != null ? message.getSender().getNickname() : null;

        return new ChatMessageBroadcast(
                message.getId(),
                message.getChatRoom().getId(),
                message.getType().name(),
                senderId,
                senderNickname,
                message.getContent(),
                message.getCreatedAt()
        );
    }

    // 읽음 이벤트 — 상대방 화면의 안읽음 수 실시간 제거용
    public static ChatMessageBroadcast readEvent(Long roomId, Long memberId) {
        return new ChatMessageBroadcast(null, roomId, "READ", memberId, null, null, LocalDateTime.now());
    }
}
