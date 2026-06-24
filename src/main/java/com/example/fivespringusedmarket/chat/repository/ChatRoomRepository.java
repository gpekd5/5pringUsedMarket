package com.example.fivespringusedmarket.chat.repository;

import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /*
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

    /*
      특정 회원이 참여 중인 채팅방 목록을 최근 메시지 순으로 조회한다.
      product는 LAZY라 N+1이 발생하므로 LEFT JOIN FETCH로 미리 가져온다.
      CS 채팅 추가 시 type 필터와 countQuery 분리를 추가한다.
     */
    @Query("""
            SELECT cr FROM ChatRoom cr
            LEFT JOIN FETCH cr.product
            JOIN ChatMember cm ON cm.chatRoom = cr
            WHERE cm.member.id = :memberId
            ORDER BY CASE WHEN cr.lastMessageAt IS NULL THEN 1 ELSE 0 END ASC,
                     cr.lastMessageAt DESC, cr.createdAt DESC
            """)
    Page<ChatRoom> findRoomsByMember(
            @Param("memberId") Long memberId,
            Pageable pageable
    );
}
