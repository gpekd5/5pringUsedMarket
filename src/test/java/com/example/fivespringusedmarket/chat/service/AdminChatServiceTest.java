package com.example.fivespringusedmarket.chat.service;

import com.example.fivespringusedmarket.chat.common.ChatRoomCommonMethod;
import com.example.fivespringusedmarket.chat.dto.response.AdminEnterResponse;
import com.example.fivespringusedmarket.chat.dto.response.CsStatusUpdateResponse;
import com.example.fivespringusedmarket.chat.entity.*;
import com.example.fivespringusedmarket.chat.redis.ChatRedisPublisher;
import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.chat.repository.ChatMessageRepository;
import com.example.fivespringusedmarket.chat.repository.ChatRoomRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminChatService 단위 테스트")
class AdminChatServiceTest {

    @InjectMocks
    private AdminChatService adminChatService;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ChatRoomCommonMethod chatRoomCommonMethod;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRedisPublisher chatRedisPublisher;

    private Member admin;
    private ChatRoom csRoom;

    @BeforeEach
    void setUp() {
        admin = Member.create("admin@test.com", "encoded", "관리자");
        ReflectionTestUtils.setField(admin, "id", 1L);

        csRoom = ChatRoom.createCsRoom("테스트 문의");
        ReflectionTestUtils.setField(csRoom, "id", 10L);
    }

    @Test
    @DisplayName("관리자 입장 성공 - WAITING 방 입장 시 IN_PROGRESS 전이")
    void adminEnterCsRoom_success() {
        given(chatRoomRepository.findByIdWithLock(10L)).willReturn(Optional.of(csRoom));
        given(chatRoomCommonMethod.getMemberOrThrow(1L)).willReturn(admin);
        given(chatMemberRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        AdminEnterResponse response = adminChatService.adminEnterCsRoom(10L, 1L);

        assertThat(response.roomId()).isEqualTo(10L);
        assertThat(response.csStatus()).isEqualTo("IN_PROGRESS");
        assertThat(csRoom.getCsStatus()).isEqualTo(CsStatus.IN_PROGRESS);
        verify(chatMemberRepository).save(any(ChatMember.class));
    }

    @Test
    @DisplayName("관리자 입장 실패 - CS 타입이 아닌 방")
    void adminEnterCsRoom_notCsRoom_throwsException() {
        ChatRoom tradeRoom = ChatRoom.createCsRoom("거래테스트방");
        ReflectionTestUtils.setField(tradeRoom, "id", 20L);
        ReflectionTestUtils.setField(tradeRoom, "type", com.example.fivespringusedmarket.chat.entity.ChatRoomType.TRADE);
        ReflectionTestUtils.setField(tradeRoom, "csStatus", null);
        given(chatRoomRepository.findByIdWithLock(20L)).willReturn(Optional.of(tradeRoom));

        assertThatThrownBy(() -> adminChatService.adminEnterCsRoom(20L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CS_ROOM);
    }

    @Test
    @DisplayName("관리자 입장 실패 - 이미 IN_PROGRESS 상태인 방")
    void adminEnterCsRoom_alreadyInProgress_throwsException() {
        csRoom.changeCsStatus(CsStatus.IN_PROGRESS);
        given(chatRoomRepository.findByIdWithLock(10L)).willReturn(Optional.of(csRoom));

        assertThatThrownBy(() -> adminChatService.adminEnterCsRoom(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CS_ALREADY_IN_PROGRESS);
    }

    @Test
    @DisplayName("CS 상태 변경 성공 - STOMP 브로드캐스트 발생")
    void changeCsStatus_success_broadcastSystemMessage() {
        csRoom.changeCsStatus(CsStatus.IN_PROGRESS);
        ChatMessage systemMessage = ChatMessage.createSystem(csRoom, "문의 상태가 COMPLETED으로 변경되었습니다.");
        ReflectionTestUtils.setField(systemMessage, "id", 100L);

        given(chatRoomCommonMethod.getChatRoomOrThrow(10L)).willReturn(csRoom);
        given(chatMessageRepository.save(any())).willReturn(systemMessage);

        CsStatusUpdateResponse response = adminChatService.changeCsStatus(10L, CsStatus.COMPLETED);

        assertThat(response.roomId()).isEqualTo(10L);
        assertThat(response.csStatus()).isEqualTo("COMPLETED");
        assertThat(csRoom.getCsStatus()).isEqualTo(CsStatus.COMPLETED);
        verify(chatRedisPublisher).publish(
                10L,
                com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast.from(systemMessage)
        );
    }

    @Test
    @DisplayName("CS 상태 변경 성공 - 고객 unreadCount 증가")
    void changeCsStatus_success_incrementsCustomerUnreadCount() {
        csRoom.changeCsStatus(CsStatus.IN_PROGRESS);

        Member customer = Member.create("customer@test.com", "encoded", "고객");
        ReflectionTestUtils.setField(customer, "id", 2L);
        ChatMember customerMember = ChatMember.create(csRoom, customer, ChatMemberRole.MEMBER);
        ChatMember adminMember = ChatMember.create(csRoom, admin, ChatMemberRole.ADMIN);

        ChatMessage systemMessage = ChatMessage.createSystem(csRoom, "문의 상태가 COMPLETED으로 변경되었습니다.");
        ReflectionTestUtils.setField(systemMessage, "id", 200L);

        given(chatRoomCommonMethod.getChatRoomOrThrow(10L)).willReturn(csRoom);
        given(chatMessageRepository.save(any())).willReturn(systemMessage);
        given(chatMemberRepository.findByChatRoomIdWithMember(10L)).willReturn(List.of(customerMember, adminMember));

        long beforeUnread = customerMember.getUnreadCount();
        adminChatService.changeCsStatus(10L, CsStatus.COMPLETED);

        assertThat(customerMember.getUnreadCount()).isEqualTo(beforeUnread + 1);
        assertThat(adminMember.getUnreadCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("CS 상태 변경 실패 - CS 타입이 아닌 방")
    void changeCsStatus_notCsRoom_throwsException() {
        ChatRoom tradeRoom = ChatRoom.createCsRoom("거래테스트방");
        ReflectionTestUtils.setField(tradeRoom, "id", 20L);
        ReflectionTestUtils.setField(tradeRoom, "type", com.example.fivespringusedmarket.chat.entity.ChatRoomType.TRADE);
        ReflectionTestUtils.setField(tradeRoom, "csStatus", null);
        given(chatRoomCommonMethod.getChatRoomOrThrow(20L)).willReturn(tradeRoom);

        assertThatThrownBy(() -> adminChatService.changeCsStatus(20L, CsStatus.IN_PROGRESS))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CS_ROOM);
    }
}
