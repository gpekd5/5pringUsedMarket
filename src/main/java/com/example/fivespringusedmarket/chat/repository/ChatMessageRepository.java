package com.example.fivespringusedmarket.chat.repository;

import com.example.fivespringusedmarket.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 안읽음 수 계산: lastReadMessageId 이후 메시지 개수
    long countByChatRoomIdAndIdGreaterThan(Long chatRoomId, Long lastReadMessageId);

    // 커서 기반 페이징: lastMessageId보다 작은 메시지를 최신순으로 조회한다.
    // sender LEFT JOIN FETCH로 N+1 방지. size+1개 조회해서 hasNext 여부를 판단한다.
    @Query("""
            SELECT m FROM ChatMessage m
            LEFT JOIN FETCH m.sender
            WHERE m.chatRoom.id = :roomId
            AND (:lastMessageId IS NULL OR m.id < :lastMessageId)
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findMessagesByCursor(
            @Param("roomId") Long roomId,
            @Param("lastMessageId") Long lastMessageId,
            Pageable pageable
    );
}
