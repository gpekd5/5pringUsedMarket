package com.example.fivespringusedmarket.chat.repository;

import com.example.fivespringusedmarket.chat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMemberRepository extends JpaRepository<ChatMember,Long> {

    /**
      채팅방 목록 조회 시 N+1을 줄이기 위해 여러 방의 ChatMember를 한 번에 조회
     */
    @Query("""
            SELECT cm FROM ChatMember cm
            JOIN FETCH cm.member
            WHERE cm.chatRoom.id IN :roomIds
            """)
    List<ChatMember> findByChatRoomIdInWithMember(@Param("roomIds") List<Long> roomIds);

    boolean existsByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    @Query("SELECT cm FROM ChatMember cm JOIN FETCH cm.member WHERE cm.chatRoom.id = :roomId")
    List<ChatMember> findByChatRoomIdWithMember(@Param("roomId") Long roomId);
}
