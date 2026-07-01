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
        LocalDateTime createdAt,
        String csStatus   // CS 상태 변경 이벤트에서만 값 있음, 나머지는 null
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
                message.getCreatedAt(),
                null
        );
    }

    // CS 상태 변경 이벤트 — 클라이언트가 csStatus 값으로 UI 상태 전환에 사용
    public static ChatMessageBroadcast csStatusEvent(ChatMessage systemMessage, String csStatus) {
        Long senderId = systemMessage.getSender() != null ? systemMessage.getSender().getId() : null;
        String senderNickname = systemMessage.getSender() != null ? systemMessage.getSender().getNickname() : null;

        return new ChatMessageBroadcast(
                systemMessage.getId(),
                systemMessage.getChatRoom().getId(),
                systemMessage.getType().name(),
                senderId,
                senderNickname,
                systemMessage.getContent(),
                systemMessage.getCreatedAt(),
                csStatus
        );
    }

    // 읽음 이벤트 — 상대방 화면의 안읽음 수 실시간 제거용
    public static ChatMessageBroadcast readEvent(Long roomId, Long memberId) {
        return new ChatMessageBroadcast(null, roomId, "READ", memberId, null, null, LocalDateTime.now(), null);
    }
}
