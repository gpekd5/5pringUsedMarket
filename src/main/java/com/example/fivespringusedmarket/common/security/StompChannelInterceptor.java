package com.example.fivespringusedmarket.common.security;

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
  HTTP Filter 대신 여기서 WebSocket 인증을 처리한다.
  이후 @MessageMapping 핸들러에서는 Principal.getName()으로 memberId를 꺼낸다.
 */
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // CONNECT 프레임에서만 JWT를 검증한다.
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            String token = extractToken(authorization);

            try {
                jwtUtil.isValidToken(token);
                AuthMember authMember = jwtUtil.extractAuthMember(token);
                // memberId를 Principal.name에 저장해 이후 핸들러에서 사용한다.
                accessor.setUser(new StompPrincipal(String.valueOf(authMember.memberId())));
            } catch (CustomException e) {
                // 인증 실패 시 연결을 끊는다.
                throw new IllegalArgumentException("STOMP 인증 실패: " + e.getMessage(), e);
            }
        }

        return message;
    }

    private String extractToken(String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        throw new IllegalArgumentException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
    }
}
