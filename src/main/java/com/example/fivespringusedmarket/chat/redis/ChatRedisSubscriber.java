package com.example.fivespringusedmarket.chat.redis;

import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/*
  Redis 채널에서 메시지를 수신해 이 서버의 STOMP 구독자에게 브로드캐스트한다.
  PatternTopic("chat-room:*")으로 모든 채팅방 채널을 단일 구독으로 처리한다.
  pattern은 구독 패턴(chat-room:*)이므로 roomId는 message.getChannel()에서 파싱한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber implements MessageListener {

    private static final String CHAT_CHANNEL_PREFIX = "chat-room:";
    private static final String STOMP_DESTINATION_PREFIX = "/sub/chat/rooms/";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer container;

    @PostConstruct
    public void init() {
        container.addMessageListener(this, new PatternTopic(CHAT_CHANNEL_PREFIX + "*"));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // pattern은 구독 패턴(chat-room:*)이므로 실제 채널명은 message.getChannel()에서 가져온다
        String channel = new String(message.getChannel());
        String roomId = channel.substring(CHAT_CHANNEL_PREFIX.length());

        try {
            ChatMessageBroadcast broadcast = objectMapper.readValue(message.getBody(), ChatMessageBroadcast.class);
            messagingTemplate.convertAndSend(STOMP_DESTINATION_PREFIX + roomId, broadcast);
        } catch (Exception e) {
            log.error("Redis 메시지 역직렬화 실패. channel={}", channel, e);
        }
    }
}
