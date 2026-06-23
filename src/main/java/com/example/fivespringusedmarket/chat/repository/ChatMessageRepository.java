package com.example.fivespringusedmarket.chat.repository;

import com.example.fivespringusedmarket.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 안읽음 수 계산: lastReadMessageId 이후 메시지 개수
    long countByChatRoomIdAndIdGreaterThan(Long chatRoomId, Long lastReadMessageId);
}
