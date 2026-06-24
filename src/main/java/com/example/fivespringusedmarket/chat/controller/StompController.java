package com.example.fivespringusedmarket.chat.controller;

import com.example.fivespringusedmarket.chat.dto.request.ChatSendRequest;
import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import com.example.fivespringusedmarket.chat.service.StompService;
import com.example.fivespringusedmarket.common.config.StompSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
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
    private final StompSessionRegistry sessionRegistry;

    /**
     채팅방 메시지를 수신하고 type에 따라 처리를 분기한다.
     클라이언트 전송 경로: /pub/chat/{roomId}
     브로드캐스트 경로: /sub/chat/{roomId}
     */
    @MessageMapping("/chat/{roomId}")
    public void handleMessage(
            @DestinationVariable Long roomId,
            @Payload ChatSendRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        // Principal.name에는 memberId가 담겨있다. ChannelInterceptor에서 설정한다.
        Long memberId = Long.parseLong(principal.getName());
        String sessionId = headerAccessor.getSessionId();

        ChatMessageBroadcast broadcast = switch (request.type()) {
            case TALK -> stompService.sendMessage(roomId, memberId, request);
            case ENTER -> {
                // 입장 시 세션 등록. 비정상 퇴장이 발생하면 WebSocketEventListener가 이 정보로 처리한다.
                sessionRegistry.register(sessionId, memberId, roomId);
                yield stompService.enterRoom(roomId, memberId);
            }
            case LEAVE -> {
                // 정상 퇴장 시 세션 해제. WebSocketEventListener가 중복 처리하지 않도록 한다.
                sessionRegistry.unregister(sessionId);
                yield stompService.leaveRoom(roomId, memberId);
            }
        };

        messagingTemplate.convertAndSend("/sub/chat/" + roomId, broadcast);
    }
}

