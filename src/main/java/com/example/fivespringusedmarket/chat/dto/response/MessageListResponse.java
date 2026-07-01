package com.example.fivespringusedmarket.chat.dto.response;

import com.example.fivespringusedmarket.chat.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
  메시지 목록 조회 응답 DTO
  hasNext로 스크롤 올려 추가 조회 가능 여부를 나타낸다.
  nextCursorId는 다음 요청 시 lastMessageId로 사용한다.
 */
//메세지목록 전체
public record MessageListResponse(
        List<MessageItem> messages,
        boolean hasNext,
        Long nextCursorId
) {
    //메세지 하나의 내용
    public record MessageItem(
            Long messageId,
            String type,
            String content,
            Long senderId,
            String senderNickname,
            LocalDateTime createdAt
    ) {
        public static MessageItem from(ChatMessage message) {
            return new MessageItem(
                    message.getId(),
                    message.getType().name(),
                    message.getContent(),
                    message.getSender() != null ? message.getSender().getId() : null,
                    message.getSender() != null ? message.getSender().getNickname() : null,
                    message.getCreatedAt()
            );
        }
    }

    public static MessageListResponse of(List<ChatMessage> messages, int size) {
        boolean hasNext = messages.size() > size;
        List<ChatMessage> result = hasNext ? messages.subList(0, size) : messages;
        Long nextCursorId = hasNext ? result.get(result.size() - 1).getId() : null;

        return new MessageListResponse(
                result.stream().map(MessageItem::from).toList(),
                hasNext,
                nextCursorId
        );
    }
}
