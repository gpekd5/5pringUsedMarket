package com.example.fivespringusedmarket.chat.dto.response;

import com.example.fivespringusedmarket.chat.entity.ChatMember;
import com.example.fivespringusedmarket.chat.entity.ChatMemberRole;
import com.example.fivespringusedmarket.chat.entity.ChatRoom;

import java.time.LocalDateTime;
import java.util.List;

/*
  관리자용 CS 채팅방 목록 응답 DTO
  고객 닉네임은 ChatMember 중 MEMBER 역할인 사람으로 식별한다
 */
public record CsRoomListResponse(
        Long roomId,
        String title,
        String csStatus,
        String lastMessage,
        LocalDateTime lastMessageAt,
        String customerNickname
) {
    public static CsRoomListResponse of(ChatRoom room, List<ChatMember> roomMembers) {
        String customerNickname = roomMembers.stream()
                .filter(cm -> cm.getMemberRole() == ChatMemberRole.MEMBER)
                .findFirst()
                .map(cm -> cm.getMember().getNickname())
                .orElse(null);

        return new CsRoomListResponse(
                room.getId(),
                room.getTitle(),
                room.getCsStatus().name(),
                room.getLastMessageContent(),
                room.getLastMessageAt(),
                customerNickname
        );
    }
}
