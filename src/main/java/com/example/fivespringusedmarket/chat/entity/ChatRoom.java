package com.example.fivespringusedmarket.chat.entity;

import com.example.fivespringusedmarket.common.entity.BaseEntity;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 엔티티다.
 * TRADE(거래 채팅)와 CS(문의 채팅) 두 유형을 하나의 테이블로 관리한다.
 */
@Getter
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatRoomType type;

    @Column(nullable = false, length = 200)
    private String title;

    // TRADE 전용. CS 채팅방은 null이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // CS 전용. TRADE 채팅방은 null이다.
    @Enumerated(EnumType.STRING)
    @Column(name = "cs_status", length = 20)
    private CsStatus csStatus;

    // 마지막 메시지 전송 시각. 채팅방 목록 정렬 기준으로 사용한다.
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
    //마지막 메세지
    @Column(name = "last_message_content", length = 500)
    private String lastMessageContent;

    /**
     * 거래 채팅방을 생성한다.
     * 상품명을 채팅방 제목으로 사용한다.
     */
    public static ChatRoom createTradeRoom(Product product) {
        ChatRoom room = new ChatRoom();
        room.type = ChatRoomType.TRADE;
        room.title = product.getTitle();
        room.product = product;
        return room;
    }

    public static ChatRoom createCsRoom(String title) {
        ChatRoom room = new ChatRoom();
        room.type = ChatRoomType.CS;
        room.title = title;
        room.csStatus = CsStatus.WAITING;
        return room;
    }

    /*
     CS 상태를 전이한다.
     허용 전이: WAITING→IN_PROGRESS, IN_PROGRESS→COMPLETED
     허용하지 않는 전이: WAITING→COMPLETED, IN_PROGRESS→WAITING, COMPLETED→모든 상태
     */
    public void changeCsStatus(CsStatus newStatus) {
        if (this.csStatus == CsStatus.COMPLETED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (this.csStatus == CsStatus.WAITING && newStatus == CsStatus.COMPLETED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (this.csStatus == CsStatus.IN_PROGRESS && newStatus == CsStatus.WAITING) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.csStatus = newStatus;
    }

    //메시지가 전송될 때마다 lastMessageAt을 갱신
    public void updateLastMessage(String content, LocalDateTime sentAt) {
        this.lastMessageContent = content;
        this.lastMessageAt = sentAt;
    }
}
