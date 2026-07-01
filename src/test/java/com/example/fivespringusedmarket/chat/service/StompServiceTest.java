package com.example.fivespringusedmarket.chat.service;

import com.example.fivespringusedmarket.chat.common.ChatRoomCommonMethod;
import com.example.fivespringusedmarket.chat.dto.request.ChatSendRequest;
import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import com.example.fivespringusedmarket.chat.entity.*;
import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.chat.repository.ChatMessageRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StompService 단위 테스트")
class StompServiceTest {

    @InjectMocks
    private StompService stompService;

    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomCommonMethod chatRoomCommonMethod;

    private Member sender;
    private Member receiver;
    private ChatRoom tradeRoom;
    private ChatRoom csRoom;
    private ChatMember senderMember;
    private ChatMember receiverMember;

    @BeforeEach
    void setUp() {
        sender = Member.create("sender@test.com", "encoded", "발신자");
        ReflectionTestUtils.setField(sender, "id", 1L);

        receiver = Member.create("receiver@test.com", "encoded", "수신자");
        ReflectionTestUtils.setField(receiver, "id", 2L);

        tradeRoom = ChatRoom.createCsRoom("거래테스트방");
        ReflectionTestUtils.setField(tradeRoom, "id", 10L);
        ReflectionTestUtils.setField(tradeRoom, "type", com.example.fivespringusedmarket.chat.entity.ChatRoomType.TRADE);
        ReflectionTestUtils.setField(tradeRoom, "csStatus", null);

        csRoom = ChatRoom.createCsRoom("문의");
        ReflectionTestUtils.setField(csRoom, "id", 20L);

        senderMember = ChatMember.create(tradeRoom, sender, ChatMemberRole.MEMBER);
        receiverMember = ChatMember.create(tradeRoom, receiver, ChatMemberRole.MEMBER);
    }

    @Test
    @DisplayName("TALK 메시지 전송 성공 - 상대방 unreadCount 증가")
    void sendMessage_success_incrementsReceiverUnreadCount() {
        ChatSendRequest request = new ChatSendRequest(MessageType.TALK, "안녕하세요");
        ChatMessage message = ChatMessage.createTalk(tradeRoom, sender, "안녕하세요");
        ReflectionTestUtils.setField(message, "id", 1L);

        given(chatRoomCommonMethod.getChatRoomOrThrow(10L)).willReturn(tradeRoom);
        given(chatRoomCommonMethod.getMemberOrThrow(1L)).willReturn(sender);
        given(chatMemberRepository.findByChatRoomIdWithMember(10L)).willReturn(List.of(senderMember, receiverMember));
        given(chatMessageRepository.save(any())).willReturn(message);

        long beforeUnread = receiverMember.getUnreadCount();
        ChatMessageBroadcast broadcast = stompService.sendMessage(10L, 1L, request);

        assertThat(broadcast.content()).isEqualTo("안녕하세요");
        assertThat(receiverMember.getUnreadCount()).isEqualTo(beforeUnread + 1);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("COMPLETED 상태 CS 방에서 메시지 전송 시 예외 발생")
    void sendMessage_completedCsRoom_throwsException() {
        csRoom.changeCsStatus(CsStatus.IN_PROGRESS);
        csRoom.changeCsStatus(CsStatus.COMPLETED);
        ChatSendRequest request = new ChatSendRequest(MessageType.TALK, "추가 문의입니다");

        given(chatRoomCommonMethod.getChatRoomOrThrow(20L)).willReturn(csRoom);

        assertThatThrownBy(() -> stompService.sendMessage(20L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_COMPLETED);
    }

    @Test
    @DisplayName("메시지 전송 시 lastMessageContent, lastMessageAt 갱신")
    void sendMessage_updatesLastMessage() {
        ChatSendRequest request = new ChatSendRequest(MessageType.TALK, "마지막 메시지");
        ChatMessage message = ChatMessage.createTalk(tradeRoom, sender, "마지막 메시지");
        ReflectionTestUtils.setField(message, "id", 1L);
        ReflectionTestUtils.setField(message, "createdAt", java.time.LocalDateTime.now());

        given(chatRoomCommonMethod.getChatRoomOrThrow(10L)).willReturn(tradeRoom);
        given(chatRoomCommonMethod.getMemberOrThrow(1L)).willReturn(sender);
        given(chatMemberRepository.findByChatRoomIdWithMember(10L)).willReturn(List.of(senderMember, receiverMember));
        given(chatMessageRepository.save(any())).willReturn(message);

        stompService.sendMessage(10L, 1L, request);

        assertThat(tradeRoom.getLastMessageContent()).isEqualTo("마지막 메시지");
        assertThat(tradeRoom.getLastMessageAt()).isNotNull();
    }
}
