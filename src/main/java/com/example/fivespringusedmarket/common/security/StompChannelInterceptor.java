package com.example.fivespringusedmarket.common.security;

import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
  STOMP CONNECT 시점에 JWT를 검증하고 Principal을 설정하는 인터셉터다.
  SUBSCRIBE 시점에 roomId를 파싱해 채팅방 참여자 여부를 검증한다.
  HTTP Filter 대신 여기서 WebSocket 인증을 처리한다.
 */
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    // /sub/chat/rooms/{roomId} 형식
    private static final String SUBSCRIBE_DESTINATION_PREFIX = "/sub/chat/rooms/";

    private final JwtUtil jwtUtil;
    private final ChatMemberRepository chatMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            String token = extractToken(authorization);

            try {
                jwtUtil.isValidToken(token);
                AuthMember authMember = jwtUtil.extractAuthMember(token);
                accessor.setUser(new StompPrincipal(String.valueOf(authMember.memberId())));
            } catch (CustomException e) {
                throw new IllegalArgumentException("STOMP 인증 실패: " + e.getMessage(), e);
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith(SUBSCRIBE_DESTINATION_PREFIX)) {
                Long roomId = parseRoomId(destination);
                Long memberId = getMemberId(accessor);
                if (!chatMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId)) {
                    throw new IllegalArgumentException("채팅방 구독 권한이 없습니다.");
                }
            }
        }

        return message;
    }

    private Long parseRoomId(String destination) {
        try {
            return Long.parseLong(destination.substring(SUBSCRIBE_DESTINATION_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 구독 경로입니다: " + destination);
        }
    }

    private Long getMemberId(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        return Long.parseLong(accessor.getUser().getName());
    }

    private String extractToken(String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        throw new IllegalArgumentException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
    }
}
