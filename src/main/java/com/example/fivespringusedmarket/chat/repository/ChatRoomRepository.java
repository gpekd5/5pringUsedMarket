package com.example.fivespringusedmarket.chat.repository;

import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import com.example.fivespringusedmarket.chat.entity.CsStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /*
      관리자용 CS 채팅방 목록 조회
      csStatus가 null이면 전체, 아니면 해당 상태만 필터링한다
      최근 메시지 순 정렬, MySQL NULLS LAST 미지원으로 CASE WHEN 처리
     */
    @Query("""
            SELECT cr FROM ChatRoom cr
            WHERE cr.type = 'CS'
            AND (:csStatus IS NULL OR cr.csStatus = :csStatus)
            ORDER BY CASE WHEN cr.lastMessageAt IS NULL THEN 1 ELSE 0 END ASC,
                     cr.lastMessageAt DESC, cr.createdAt DESC
            """)
    Page<ChatRoom> findCsRooms(
            @Param("csStatus") CsStatus csStatus,
            Pageable pageable
    );

    /*
      CS 채팅방을 비관적 쓰기 락으로 조회
      관리자 동시 입장 시 한 명만 처리되도록 행 수준 락을 건다
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.id = :roomId")
    Optional<ChatRoom> findByIdWithLock(@Param("roomId") Long roomId);
}
