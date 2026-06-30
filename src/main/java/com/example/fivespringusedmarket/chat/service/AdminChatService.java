package com.example.fivespringusedmarket.chat.service;

import com.example.fivespringusedmarket.chat.common.ChatRoomCommonMethod;
import com.example.fivespringusedmarket.chat.dto.response.AdminEnterResponse;
import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import com.example.fivespringusedmarket.chat.dto.response.CsRoomListResponse;
import com.example.fivespringusedmarket.chat.dto.response.CsStatusUpdateResponse;
import com.example.fivespringusedmarket.chat.entity.*;
import com.example.fivespringusedmarket.chat.redis.ChatRedisPublisher;
import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.chat.repository.ChatMessageRepository;
import com.example.fivespringusedmarket.chat.repository.ChatRoomRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatRoomCommonMethod chatRoomCommonMethod;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRedisPublisher chatRedisPublisher;

    @Transactional
    public AdminEnterResponse adminEnterCsRoom(Long roomId, Long adminId) {
        // 동시 입장 방지: SELECT FOR UPDATE로 행을 잠근다.
        ChatRoom room = chatRoomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (room.getType() != ChatRoomType.CS) {
            throw new CustomException(ErrorCode.NOT_CS_ROOM);
        }

        // WAITING 상태가 아니면 이미 다른 관리자가 처리 중이거나 완료된 방이다.
        if (room.getCsStatus() != CsStatus.WAITING) {
            throw new CustomException(ErrorCode.CS_ALREADY_IN_PROGRESS);
        }

        Member admin = chatRoomCommonMethod.getMemberOrThrow(adminId);
        chatMemberRepository.save(ChatMember.create(room, admin, ChatMemberRole.ADMIN));

        // 입장과 동시에 IN_PROGRESS로 자동 전이한다.
        room.changeCsStatus(CsStatus.IN_PROGRESS);

        return new AdminEnterResponse(room.getId(), room.getTitle(), room.getCsStatus().name());
    }

    /*
      CS 채팅방의 상태를 변경
      ADMIN 전용, 상태 변경 후 고객에게 STOMP 알림을 발송
     */
    @Transactional
    public CsStatusUpdateResponse changeCsStatus(Long roomId, CsStatus newStatus) {
        ChatRoom room = chatRoomCommonMethod.getChatRoomOrThrow(roomId);

        if (room.getType() != ChatRoomType.CS) {
            throw new CustomException(ErrorCode.NOT_CS_ROOM);
        }
        // 상태 전이 유효성은 엔티티 내부에서 검증한다.
        room.changeCsStatus(newStatus);
        // 시스템 메시지 저장
        ChatMessage systemMessage = chatMessageRepository.save(
                ChatMessage.createSystem(room, "문의 상태가 " + newStatus.name() + "으로 변경되었습니다.")
        );
        room.updateLastMessage(systemMessage.getContent(), systemMessage.getCreatedAt());

        // unreadCount 증가 — 상태 변경 알림을 오프라인 고객이 놓치지 않도록 한다
        chatMemberRepository.findByChatRoomIdWithMember(roomId).stream()
                .filter(cm -> cm.getMemberRole() == ChatMemberRole.MEMBER)
                .findFirst()
                .ifPresent(ChatMember::incrementUnreadCount);

        // Redis Pub/Sub으로 브로드캐스트 — 다중 서버 환경에서 모든 인스턴스에 전달된다
        chatRedisPublisher.publish(roomId, ChatMessageBroadcast.from(systemMessage));
        return new CsStatusUpdateResponse(roomId, newStatus.name());
    }

    /*
      관리자용 CS 채팅방 목록 조회
      csStatus가 null이면 전체, 값이 있으면 해당 상태만 필터링한다
      N+1 방지: roomId 목록으로 ChatMember를 한 번에 배치 조회한다
     */
    @Transactional(readOnly = true)
    public Page<CsRoomListResponse> getCsRooms(CsStatus csStatus, Pageable pageable) {
        Page<ChatRoom> rooms = chatRoomRepository.findCsRooms(csStatus, pageable);

        List<Long> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        if (roomIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<ChatMember> allMembers = chatMemberRepository.findByChatRoomIdInWithMember(roomIds);

        return rooms.map(room -> {
            List<ChatMember> roomMembers = allMembers.stream()
                    .filter(cm -> cm.getChatRoom().getId().equals(room.getId()))
                    .toList();
            return CsRoomListResponse.of(room, roomMembers);
        });
    }
}
