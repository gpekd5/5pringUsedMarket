package com.example.fivespringusedmarket.chat.redis;

import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRedisPublisher 단위 테스트")
class ChatRedisPublisherTest {

    @InjectMocks
    private ChatRedisPublisher chatRedisPublisher;

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ObjectMapper objectMapper;

    @Test
    @DisplayName("publish 성공 - 단일 채팅 이벤트 채널에 JSON 문자열 발행")
    void publish_success_sendsJsonToCorrectChannel() throws Exception {
        ChatMessageBroadcast broadcast = new ChatMessageBroadcast(
                1L, 5L, "TALK", 10L, "테스터", "안녕하세요", LocalDateTime.now(), null
        );
        given(objectMapper.writeValueAsString(broadcast)).willReturn("{\"content\":\"안녕하세요\"}");

        chatRedisPublisher.publish(5L, broadcast);

        verify(redisTemplate).convertAndSend(eq("chat-room-events"), anyString());
    }

    @Test
    @DisplayName("publish 성공 - ObjectMapper로 직렬화된 JSON이 발행됨")
    void publish_success_usesObjectMapperSerialization() throws Exception {
        ChatMessageBroadcast broadcast = new ChatMessageBroadcast(
                2L, 3L, "SYSTEM", null, null, "상태가 변경되었습니다.", LocalDateTime.now(), null
        );
        String expectedJson = "{\"messageId\":2,\"content\":\"상태가 변경되었습니다.\"}";
        given(objectMapper.writeValueAsString(broadcast)).willReturn(expectedJson);

        chatRedisPublisher.publish(3L, broadcast);

        verify(redisTemplate).convertAndSend("chat-room-events", expectedJson);
    }

    @Test
    @DisplayName("publish 성공 - roomId가 다른 메시지도 단일 채널로 발행됨")
    void publish_success_usesSingleEventChannel() throws Exception {
        ChatMessageBroadcast broadcastRoom1 = new ChatMessageBroadcast(
                1L, 1L, "TALK", 10L, "유저A", "방1 메시지", LocalDateTime.now(), null
        );
        ChatMessageBroadcast broadcastRoom2 = new ChatMessageBroadcast(
                2L, 2L, "TALK", 20L, "유저B", "방2 메시지", LocalDateTime.now(), null
        );
        given(objectMapper.writeValueAsString(broadcastRoom1)).willReturn("{\"roomId\":1}");
        given(objectMapper.writeValueAsString(broadcastRoom2)).willReturn("{\"roomId\":2}");

        chatRedisPublisher.publish(1L, broadcastRoom1);
        chatRedisPublisher.publish(2L, broadcastRoom2);

        verify(redisTemplate, org.mockito.Mockito.times(2)).convertAndSend(eq("chat-room-events"), anyString());
    }
}
