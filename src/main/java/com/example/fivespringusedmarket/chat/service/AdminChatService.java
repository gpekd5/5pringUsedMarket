package com.example.fivespringusedmarket.chat.service;

import com.example.fivespringusedmarket.chat.common.ChatRoomCommonMethod;
import com.example.fivespringusedmarket.chat.dto.response.AdminEnterResponse;
import com.example.fivespringusedmarket.chat.entity.*;
import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.chat.repository.ChatRoomRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatRoomCommonMethod chatRoomCommonMethod;

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
}
