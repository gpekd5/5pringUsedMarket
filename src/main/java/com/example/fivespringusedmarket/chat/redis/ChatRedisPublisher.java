package com.example.fivespringusedmarket.chat.redis;

import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/*
  채팅 메시지를 Redis 채널에 발행한다.
  ObjectMapper로 JSON 문자열로 직렬화한 뒤 발행해 deprecated serializer를 사용하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private static final String CHAT_CHANNEL = "chat-room-events";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void publish(Long roomId, ChatMessageBroadcast broadcast) {
        String json = objectMapper.writeValueAsString(broadcast);
        redisTemplate.convertAndSend(CHAT_CHANNEL, json);
    }
}
