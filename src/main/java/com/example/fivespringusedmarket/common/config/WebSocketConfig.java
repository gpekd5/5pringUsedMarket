package com.example.fivespringusedmarket.common.config;

import com.example.fivespringusedmarket.common.security.StompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
  STOMP over WebSocket 설정이다.
  클라이언트는 /ws-chat 엔드포인트로 연결하고,
  /pub 접두사로 메시지를 보내고 /sub 접두사로 구독한다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompChannelInterceptor stompChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독할 경로 접두사
        registry.enableSimpleBroker("/sub");
        // 클라이언트가 메시지를 전송할 경로 접두사
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                // 개발 중 모든 Origin 허용. 운영에서는 실제 도메인으로 교체한다.
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // STOMP CONNECT 시 JWT 검증을 수행하는 인터셉터를 등록한다.
        registration.interceptors(stompChannelInterceptor);
    }
}
