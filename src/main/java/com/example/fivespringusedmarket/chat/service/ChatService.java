package com.example.fivespringusedmarket.chat.service;

import com.example.fivespringusedmarket.chat.dto.request.CsChatRoomCreateRequest;
import com.example.fivespringusedmarket.chat.dto.request.TradeChatRoomCreateRequest;
import com.example.fivespringusedmarket.chat.dto.response.ChatRoomListResponse;
import com.example.fivespringusedmarket.chat.dto.response.CsChatRoomCreateResponse;
import com.example.fivespringusedmarket.chat.dto.response.TradeChatRoomCreateResponse;
import com.example.fivespringusedmarket.chat.entity.ChatMember;
import com.example.fivespringusedmarket.chat.entity.ChatMemberRole;
import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.chat.repository.ChatMessageRepository;
import com.example.fivespringusedmarket.chat.repository.ChatRoomRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public TradeChatRoomCreateResponse findOrCreateTradeRoom(Long buyerId, TradeChatRoomCreateRequest request) {
        Product product = getProductOrThrow(request.productId());

        if (product.getSeller().getId().equals(buyerId)) {
            throw new CustomException(ErrorCode.PRODUCT_OWNER_CANNOT_CHAT);
        }

        if (product.getStatus() == ProductStatus.SOLD) {
            throw new CustomException(ErrorCode.PRODUCT_SOLD_OUT);
        }

        Member seller = product.getSeller();
        Member buyer = getMemberOrThrow(buyerId);

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
        Member customer = getMemberOrThrow(customerId);
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

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
