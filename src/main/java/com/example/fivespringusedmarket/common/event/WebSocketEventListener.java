package com.example.fivespringusedmarket.common.event;

import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import com.example.fivespringusedmarket.chat.service.ChatService;
import com.example.fivespringusedmarket.chat.service.StompService;
import com.example.fivespringusedmarket.common.config.StompSessionRegistry;
import com.example.fivespringusedmarket.common.config.StompSessionRegistry.SessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
  WebSocket 세션 이벤트를 처리한다.
  네트워크 단절·앱 종료 등 비정상 퇴장 시 퇴장 시스템 메시지를 브로드캐스트한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final StompSessionRegistry sessionRegistry;
    private final StompService stompService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트 연결이 끊어지면 호출된다.
     * 정상 퇴장(STOMP LEAVE)은 StompChatController에서 처리하므로, 여기서는 세션이 남아있는 경우만 처리한다.
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        SessionInfo info = sessionRegistry.get(sessionId);

        // 정상 퇴장 시에는 StompChatController에서 이미 unregister했으므로 info가 null이다.
        if (info == null) {
            return;
        }

        sessionRegistry.unregister(sessionId);

        try {
            ChatMessageBroadcast broadcast = stompService.leaveRoom(info.roomId(), info.memberId());
            messagingTemplate.convertAndSend("/sub/chat/" + info.roomId(), broadcast);
        } catch (Exception e) {
            // 퇴장 처리 실패는 채팅 서비스 전체에 영향을 주지 않도록 로그만 남긴다.
            log.warn("비정상 퇴장 처리 실패 - sessionId: {}, roomId: {}, memberId: {}",
                    sessionId, info.roomId(), info.memberId(), e);
        }
    }
}
