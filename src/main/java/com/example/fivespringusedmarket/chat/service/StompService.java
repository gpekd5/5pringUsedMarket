package com.example.fivespringusedmarket.chat.service;

import com.example.fivespringusedmarket.chat.dto.request.ChatSendRequest;
import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import com.example.fivespringusedmarket.chat.entity.*;
import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.chat.repository.ChatMessageRepository;
import com.example.fivespringusedmarket.chat.common.ChatRoomCommonMethod;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StompService {

    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomCommonMethod chatRoomCommonMethod;

    @Transactional
    public ChatMessageBroadcast sendMessage(Long roomId, Long senderId, ChatSendRequest request) {
        ChatRoom room = chatRoomCommonMethod.getChatRoomOrThrow(roomId);

        chatRoomCommonMethod.validateChatMember(roomId, senderId);

        // COMPLETED 상태 CS 채팅방에는 메시지를 전송할 수 없다.
        if (room.getType() == ChatRoomType.CS && room.getCsStatus() == CsStatus.COMPLETED) {
            throw new CustomException(ErrorCode.CHAT_COMPLETED);
        }

        Member sender = chatRoomCommonMethod.getMemberOrThrow(senderId);
        ChatMessage message = chatMessageRepository.save(ChatMessage.createTalk(room, sender, request.content()));

        // 마지막 메시지 시각 갱신으로 채팅방 목록 정렬 순서를 최신화한다.
        room.updateLastMessage(message.getContent(),message.getCreatedAt());

        // 상대방 unreadCount 증가
        chatMemberRepository.findByChatRoomIdWithMember(roomId).stream()
                .filter(cm -> !cm.getMember().getId().equals(senderId))
                .findFirst()
                .ifPresent(ChatMember::incrementUnreadCount);

        return ChatMessageBroadcast.from(message);
    }

    /*
      STOMP 입장 이벤트를 처리한다.
      시스템 메시지 저장 및 브로드캐스트만 담당한다.
      ChatMember 추가는 REST API(adminEnterCsRoom)에서 처리한다.
     */
    @Transactional
    public ChatMessageBroadcast enterRoom(Long roomId, Long memberId) {
        ChatRoom room = chatRoomCommonMethod.getChatRoomOrThrow(roomId);
        Member member = chatRoomCommonMethod.getMemberOrThrow(memberId);

        // 참여자가 아니면 입장 메시지를 보낼 수 없다.
        chatRoomCommonMethod.validateChatMember(roomId, memberId);

        ChatMessage enterMessage = chatMessageRepository.save(
                ChatMessage.createEnter(room, member.getNickname())
        );
        room.updateLastMessage(enterMessage.getContent(),enterMessage.getCreatedAt());

        return ChatMessageBroadcast.from(enterMessage);
    }

    /*
      채팅방 퇴장 이벤트를 처리한다.
      ChatMember에서 제거하지 않고 퇴장 시스템 메시지만 저장한다.
      재입장 시 기존 ChatMember 레코드를 그대로 사용한다.
     */
    @Transactional
    public ChatMessageBroadcast leaveRoom(Long roomId, Long memberId) {
        ChatRoom room = chatRoomCommonMethod.getChatRoomOrThrow(roomId);
        Member member = chatRoomCommonMethod.getMemberOrThrow(memberId);

        chatRoomCommonMethod.validateChatMember(roomId, memberId);

        ChatMessage leaveMessage = chatMessageRepository.save(
                ChatMessage.createLeave(room, member.getNickname())
        );
        room.updateLastMessage(leaveMessage.getContent(),leaveMessage.getCreatedAt());

        return ChatMessageBroadcast.from(leaveMessage);
    }
}
