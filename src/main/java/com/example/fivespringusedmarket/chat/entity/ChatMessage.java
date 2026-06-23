package com.example.fivespringusedmarket.chat.entity;

import com.example.fivespringusedmarket.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 채팅 메시지를 저장하는 JPA 엔티티다.
 * ChatRoom을 단방향 참조한다(ChatRoom에서 역방향 컬렉션 없음).
 * type 필드로 TALK / ENTER / LEAVE를 구분한다.
 * sender가 null이면 시스템 메시지(ENTER / LEAVE)다.
 * (chat_room_id, id DESC) 복합 인덱스로 커서 페이징을 최적화한다.
 */
@Getter
@Entity
@Table(
        name = "chat_messages",
        indexes = @Index(name = "idx_chat_message_room_cursor", columnList = "chat_room_id, id DESC")
) // 채팅메세지를 조회할때 커서기반 페이지네이션때문에 사용 없을시 DB가 그 수천 개를 다 읽고 나서 조건을 걸러냄
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    // nullable — ENTER / LEAVE 시스템 메시지는 null이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 일반 채팅 메시지를 생성한다.
     */
    public static ChatMessage createTalk(ChatRoom chatRoom, Member sender, String content) {
        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.sender = sender;
        message.type = MessageType.TALK;
        message.content = content;
        return message;
    }

    /**
     * 입장 시스템 메시지를 생성한다.
     */
    public static ChatMessage createEnter(ChatRoom chatRoom, String nickname) {
        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.sender = null;
        message.type = MessageType.ENTER;
        message.content = nickname + "님이 입장했습니다.";
        return message;
    }

    /**
     * 퇴장 시스템 메시지를 생성한다.
     */
    public static ChatMessage createLeave(ChatRoom chatRoom, String nickname) {
        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.sender = null;
        message.type = MessageType.LEAVE;
        message.content = nickname + "님이 퇴장했습니다.";
        return message;
    }
}