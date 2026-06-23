package com.example.fivespringusedmarket.chat.repository;

import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
      동일한 (구매자, 판매자, 상품) 조합의 거래 채팅방을 조회한다.
      TRADE 채팅방 생성 전 기존 방 여부 확인(findOrCreate)에 사용한다.
     */
    @Query("""
            SELECT cr FROM ChatRoom cr
            WHERE cr.type = 'TRADE'
            AND cr.product.id = :productId
            AND EXISTS (
                SELECT cm FROM ChatMember cm
                WHERE cm.chatRoom = cr AND cm.member.id = :buyerId
            )
            AND EXISTS (
                SELECT cm FROM ChatMember cm
                WHERE cm.chatRoom = cr AND cm.member.id = :sellerId
            )
            """)
    Optional<ChatRoom> findTradeChatRoom(
            @Param("buyerId") Long buyerId,
            @Param("sellerId") Long sellerId,
            @Param("productId") Long productId
    );
}
