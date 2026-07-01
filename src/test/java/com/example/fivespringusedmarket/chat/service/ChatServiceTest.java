package com.example.fivespringusedmarket.chat.service;

import com.example.fivespringusedmarket.chat.common.ChatRoomCommonMethod;
import com.example.fivespringusedmarket.chat.dto.request.TradeChatRoomCreateRequest;
import com.example.fivespringusedmarket.chat.dto.response.TradeChatRoomCreateResponse;
import com.example.fivespringusedmarket.chat.entity.ChatMemberRole;
import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import com.example.fivespringusedmarket.chat.redis.ChatRedisPublisher;
import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.chat.repository.ChatMessageRepository;
import com.example.fivespringusedmarket.chat.repository.ChatRoomRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.entity.MemberRole;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 단위 테스트")
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomCommonMethod chatRoomCommonMethod;
    @Mock private ChatRedisPublisher chatRedisPublisher;

    private Member seller;
    private Member buyer;
    private Member admin;
    private Product product;

    @BeforeEach
    void setUp() {
        seller = Member.create("seller@test.com", "encoded", "판매자");
        ReflectionTestUtils.setField(seller, "id", 1L);

        buyer = Member.create("buyer@test.com", "encoded", "구매자");
        ReflectionTestUtils.setField(buyer, "id", 2L);

        admin = Member.create("admin@test.com", "encoded", "관리자");
        ReflectionTestUtils.setField(admin, "id", 3L);
        ReflectionTestUtils.setField(admin, "role", MemberRole.ADMIN);

        product = Product.create(seller, "테스트 상품", "설명", 10000, ProductCategory.DIGITAL);
        ReflectionTestUtils.setField(product, "id", 100L);
    }

    @Test
    @DisplayName("거래 채팅방 신규 생성 성공")
    void findOrCreateTradeRoom_newRoom_success() {
        TradeChatRoomCreateRequest request = new TradeChatRoomCreateRequest(100L);
        ChatRoom newRoom = ChatRoom.createTradeRoom(product, 2L);
        ReflectionTestUtils.setField(newRoom, "id", 10L);

        given(chatRoomCommonMethod.getProductOrThrow(100L)).willReturn(product);
        given(chatRoomCommonMethod.getMemberOrThrow(1L)).willReturn(seller);
        given(chatRoomCommonMethod.getMemberOrThrow(2L)).willReturn(buyer);
        given(chatRoomRepository.findTradeChatRoom(2L, 1L, 100L)).willReturn(Optional.empty());
        given(chatRoomRepository.save(any())).willReturn(newRoom);
        given(chatMemberRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        TradeChatRoomCreateResponse response = chatService.findOrCreateTradeRoom(2L, request);

        assertThat(response.roomId()).isEqualTo(10L);
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatMemberRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("거래 채팅방 기존 방 반환 - 동일 조합 재요청 시 신규 생성 안 함")
    void findOrCreateTradeRoom_existingRoom_returnsExisting() {
        TradeChatRoomCreateRequest request = new TradeChatRoomCreateRequest(100L);
        ChatRoom existingRoom = ChatRoom.createTradeRoom(product, 2L);
        ReflectionTestUtils.setField(existingRoom, "id", 10L);

        given(chatRoomCommonMethod.getProductOrThrow(100L)).willReturn(product);
        given(chatRoomCommonMethod.getMemberOrThrow(1L)).willReturn(seller);
        given(chatRoomCommonMethod.getMemberOrThrow(2L)).willReturn(buyer);
        given(chatRoomRepository.findTradeChatRoom(2L, 1L, 100L)).willReturn(Optional.of(existingRoom));

        TradeChatRoomCreateResponse response = chatService.findOrCreateTradeRoom(2L, request);

        assertThat(response.roomId()).isEqualTo(10L);
        verify(chatRoomRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("본인 상품 채팅 시도 시 예외 발생")
    void findOrCreateTradeRoom_ownerCannotChat_throwsException() {
        TradeChatRoomCreateRequest request = new TradeChatRoomCreateRequest(100L);
        given(chatRoomCommonMethod.getProductOrThrow(100L)).willReturn(product);

        assertThatThrownBy(() -> chatService.findOrCreateTradeRoom(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_OWNER_CANNOT_CHAT);
    }

    @Test
    @DisplayName("판매 완료 상품 채팅 시도 시 예외 발생")
    void findOrCreateTradeRoom_soldProduct_throwsException() {
        TradeChatRoomCreateRequest request = new TradeChatRoomCreateRequest(100L);
        product.updateStatus(ProductStatus.SOLD);
        given(chatRoomCommonMethod.getProductOrThrow(100L)).willReturn(product);

        assertThatThrownBy(() -> chatService.findOrCreateTradeRoom(2L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_SOLD_OUT);
    }

    @Test
    @DisplayName("삭제된 상품 채팅 시도 시 예외 발생")
    void findOrCreateTradeRoom_deletedProduct_throwsException() {
        TradeChatRoomCreateRequest request = new TradeChatRoomCreateRequest(100L);
        product.updateStatus(ProductStatus.DELETED);
        given(chatRoomCommonMethod.getProductOrThrow(100L)).willReturn(product);

        assertThatThrownBy(() -> chatService.findOrCreateTradeRoom(2L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_SOLD_OUT);
    }

    @Test
    @DisplayName("관리자 거래 채팅 시도 시 예외 발생")
    void findOrCreateTradeRoom_adminCannotChat_throwsException() {
        TradeChatRoomCreateRequest request = new TradeChatRoomCreateRequest(100L);
        given(chatRoomCommonMethod.getProductOrThrow(100L)).willReturn(product);
        given(chatRoomCommonMethod.getMemberOrThrow(1L)).willReturn(seller);
        given(chatRoomCommonMethod.getMemberOrThrow(3L)).willReturn(admin);

        assertThatThrownBy(() -> chatService.findOrCreateTradeRoom(3L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_CANNOT_CHAT);
    }
}
