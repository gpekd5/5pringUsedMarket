package com.example.fivespringusedmarket.chat.common;

import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.chat.repository.ChatRoomRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomCommonMethod {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    //서비스 로직 공통메소드
    public ChatRoom getChatRoomOrThrow(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    public Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public void validateChatMember(Long roomId, Long memberId) {
        if (!chatMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }
    }
}
