package com.example.fivespringusedmarket.chat.service;

import com.example.fivespringusedmarket.chat.common.ChatRoomCommonMethod;
import com.example.fivespringusedmarket.chat.dto.request.CsChatRoomCreateRequest;
import com.example.fivespringusedmarket.chat.dto.request.TradeChatRoomCreateRequest;
import com.example.fivespringusedmarket.chat.dto.response.*;
import com.example.fivespringusedmarket.chat.entity.ChatMessage;
import com.example.fivespringusedmarket.chat.entity.ChatMember;
import com.example.fivespringusedmarket.chat.entity.ChatMemberRole;
import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.chat.repository.ChatMessageRepository;
import com.example.fivespringusedmarket.chat.repository.ChatRoomRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.entity.MemberRole;
import com.example.fivespringusedmarket.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomCommonMethod chatRoomCommonMethod;

    @Transactional
    public TradeChatRoomCreateResponse findOrCreateTradeRoom(Long buyerId, TradeChatRoomCreateRequest request) {
        Product product = chatRoomCommonMethod.getProductOrThrow(request.productId());

        if (product.getSeller().getId().equals(buyerId)) {
            throw new CustomException(ErrorCode.PRODUCT_OWNER_CANNOT_CHAT);
        }

        if (product.isSold() || product.isDeleted()) {
            throw new CustomException(ErrorCode.PRODUCT_SOLD_OUT);
        }

        Member seller = product.getSeller();
        Member buyer = chatRoomCommonMethod.getMemberOrThrow(buyerId);
        //관리자 구매제한
        if (buyer.getRole() == MemberRole.ADMIN) {
            throw new CustomException(ErrorCode.ADMIN_CANNOT_CHAT);
        }

        Optional<ChatRoom> existingRoom = chatRoomRepository.findTradeChatRoom(buyerId, seller.getId(), product.getId());
        if (existingRoom.isPresent()) {
            return TradeChatRoomCreateResponse.of(existingRoom.get(), product, seller);
        }

        // 신규 채팅방 및 참여자(두 명 모두 MEMBER) 동시 생성 — 하나의 트랜잭션 안에서 처리한다.
        ChatRoom room = chatRoomRepository.save(ChatRoom.createTradeRoom(product));
        chatMemberRepository.save(ChatMember.create(room, buyer, ChatMemberRole.MEMBER));
        chatMemberRepository.save(ChatMember.create(room, seller, ChatMemberRole.MEMBER));

        return TradeChatRoomCreateResponse.of(room, product, seller);
    }
    /**
      CS 문의 채팅방을 생성한다
      항상 신규 생성
     */
    @Transactional
    public CsChatRoomCreateResponse createCsRoom(Long customerId, CsChatRoomCreateRequest request) {
        Member customer = chatRoomCommonMethod.getMemberOrThrow(customerId);
        //초기상태는 WAITING
        ChatRoom room = chatRoomRepository.save(ChatRoom.createCsRoom(request.title()));

        chatMemberRepository.save(ChatMember.create(room, customer, ChatMemberRole.MEMBER));

        return CsChatRoomCreateResponse.from(room);
    }
    /**
      현재 회원이 참여 중인 채팅방 목록을 조회
      type이 null이면 TRADE, CS 전체를 반환
     */
    @Transactional(readOnly = true)
    public Page<ChatRoomListResponse> getChatRooms(Long memberId, Pageable pageable) {
        Page<ChatRoom> rooms = chatRoomRepository.findRoomsByMember(memberId, pageable);

        List<Long> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        if (roomIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // N+1 방지: 채팅방 참여자를 한 번에 조회한다.
        List<ChatMember> allMembers = chatMemberRepository.findByChatRoomIdInWithMember(roomIds);

        return rooms.map(room -> {
            ChatMember myChatMember = allMembers.stream()
                    .filter(cm -> cm.getChatRoom().getId().equals(room.getId()))
                    .filter(cm -> cm.getMember().getId().equals(memberId))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ACCESS_DENIED));

            ChatMember counterpartMember = allMembers.stream()
                    .filter(cm -> cm.getChatRoom().getId().equals(room.getId()))
                    .filter(cm -> !cm.getMember().getId().equals(memberId))
                    .findFirst()
                    .orElse(null);

            return ChatRoomListResponse.of(room, counterpartMember, myChatMember.getUnreadCount());
        });
    }
    /**
     채팅방 상세 정보를 조회
     해당 채팅방의 참여자만 조회할 수 있다
     */
    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getChatRoomDetail(Long memberId, Long roomId) {
        ChatRoom room = chatRoomCommonMethod.getChatRoomOrThrow(roomId);

        chatRoomCommonMethod.validateChatMember(roomId, memberId);

        List<ChatMember> roomMembers = chatMemberRepository.findByChatRoomIdWithMember(roomId);

        ChatMember counterpartMember = roomMembers.stream()
                .filter(cm -> !cm.getMember().getId().equals(memberId))
                .findFirst()
                .orElse(null);

        return ChatRoomDetailResponse.of(room, counterpartMember);
    }

    @Transactional(readOnly = true)
    public MessageListResponse getMessages(Long memberId, Long roomId, Long lastMessageId, int size) {
        if (!chatMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        // size+1개 조회해서 다음 페이지 존재 여부 판단
        List<ChatMessage> fetched = chatMessageRepository.findMessagesByCursor(
                roomId, lastMessageId, PageRequest.of(0, size + 1)
        );

        boolean hasNext = fetched.size() > size;
        List<ChatMessage> messages = hasNext ? fetched.subList(0, size) : fetched;

        // DESC로 가져온 메시지를 ASC(오래된 순)로 뒤집어 반환한다.
        List<ChatMessage> ordered = new ArrayList<>(messages);
        Collections.reverse(ordered);

        Long nextCursorId = hasNext ? messages.get(messages.size() - 1).getId() : null;

        return new MessageListResponse(
                ordered.stream().map(MessageListResponse.MessageItem::from).toList(),
                hasNext,
                nextCursorId
        );
    }
    /*
      채팅방 읽음 처리
      현재 사용자의 unreadCount를 0으로 리셋하고 lastReadMessageId를 최신 메시지 ID로 갱신한다.
      메시지가 없으면 아무것도 하지 않는다.
     */
    @Transactional
    public void markAsRead(Long memberId, Long roomId) {
        ChatMember chatMember = chatMemberRepository.findByChatRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ACCESS_DENIED));

        chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)
                .ifPresent(latestMessage -> {
                    chatMember.updateLastReadMessageId(latestMessage.getId());
                    chatMember.resetUnreadCount();
                });
    }
}
