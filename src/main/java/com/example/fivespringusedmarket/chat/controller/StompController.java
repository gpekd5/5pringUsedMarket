package com.example.fivespringusedmarket.chat.controller;

import com.example.fivespringusedmarket.chat.dto.request.ChatSendRequest;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import com.example.fivespringusedmarket.chat.service.StompService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 STOMP 메시지 수신 핸들러를 담당한다.
 /pub/chat/{roomId} 단일 경로로 수신하고 type 필드(TALK / ENTER / LEAVE)로 분기한다.
 ChannelInterceptor에서 검증된 Principal을 통해 발신자를 식별한다.
 */

@Controller
@RequiredArgsConstructor
public class StompController {

    private final StompService stompService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     채팅방 메시지를 수신하고 type에 따라 처리를 분기한다.
     클라이언트 전송 경로: /pub/chat/rooms/{roomId}/messages
     브로드캐스트 경로: /sub/chat/rooms/{roomId}
     */
    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void handleMessage(
            @DestinationVariable Long roomId,
            @Payload ChatSendRequest request,
            Principal principal
    ) {
        Long memberId = Long.parseLong(principal.getName());

        ChatMessageBroadcast broadcast = switch (request.type()) {
            case TALK -> stompService.sendMessage(roomId, memberId, request);
            case ENTER -> stompService.enterRoom(roomId, memberId);
            case SYSTEM -> throw new CustomException(ErrorCode.INVALID_MESSAGE_TYPE);
        };

        messagingTemplate.convertAndSend("/sub/chat/rooms/" + roomId, broadcast);
    }
}

