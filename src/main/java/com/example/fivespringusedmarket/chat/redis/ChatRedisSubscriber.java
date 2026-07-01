package com.example.fivespringusedmarket.chat.redis;

import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/*
  Redis 채널에서 메시지를 수신해 이 서버의 STOMP 구독자에게 브로드캐스트한다.
  운영 Redis가 패턴 구독을 지원하지 않을 수 있으므로 단일 채널을 구독한다.
  채팅방 구분은 메시지 본문에 포함된 roomId로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber implements MessageListener {

    static final String CHAT_CHANNEL = "chat-room-events";
    private static final String STOMP_DESTINATION_PREFIX = "/sub/chat/rooms/";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer container;

    @PostConstruct
    public void init() {
        container.addMessageListener(this, new ChannelTopic(CHAT_CHANNEL));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());

        try {
            ChatMessageBroadcast broadcast = objectMapper.readValue(message.getBody(), ChatMessageBroadcast.class);
            messagingTemplate.convertAndSend(STOMP_DESTINATION_PREFIX + broadcast.roomId(), broadcast);
        } catch (Exception e) {
            log.error("Redis 메시지 역직렬화 실패. channel={}", channel, e);
        }
    }
}
