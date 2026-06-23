package com.example.fivespringusedmarket.chat.entity;

import com.example.fivespringusedmarket.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "chat_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_chat_member",
                columnNames = {"chat_room_id", "member_id"}
        )
)
@EntityListeners(AuditingEntityListener.class) //modifiedAt 콜럼은 필요가없기때문에 사용
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 10)
    private ChatMemberRole memberRole;

    // 안읽음 수 계산 기준. 채팅방 입장(PATCH /read) 시 최신 메시지 ID로 갱신한다.
    @Column(name = "last_read_message_id", nullable = false)
    private Long lastReadMessageId = 0L;

    // 메시지 수신 시 +1, 채팅방 입장 시 0으로 리셋한다.
    @Column(name = "unread_count", nullable = false)
    private long unreadCount = 0L;

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public static ChatMember create(ChatRoom chatRoom, Member member, ChatMemberRole role) {
        ChatMember chatMember = new ChatMember();
        chatMember.chatRoom = chatRoom;
        chatMember.member = member;
        chatMember.memberRole = role;
        chatMember.lastReadMessageId = 0L;
        chatMember.unreadCount = 0L;
        return chatMember;
    }

    public void updateLastReadMessageId(Long messageId) {
        this.lastReadMessageId = messageId;
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    public void resetUnreadCount() {
        this.unreadCount = 0L;
    }
}