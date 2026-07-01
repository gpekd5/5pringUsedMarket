package com.example.fivespringusedmarket.chat.redis;

import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRedisSubscriber 단위 테스트")
class ChatRedisSubscriberTest {

    @InjectMocks
    private ChatRedisSubscriber chatRedisSubscriber;

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private RedisMessageListenerContainer container;

    @Test
    @DisplayName("init 성공 - 패턴 구독 없이 단일 채널을 구독")
    void init_success_subscribesSingleChannel() {
        chatRedisSubscriber.init();

        verify(container).addMessageListener(eq(chatRedisSubscriber), eq(new ChannelTopic("chat-room-events")));
    }

    @Test
    @DisplayName("onMessage 성공 - 메시지 본문의 roomId로 올바른 STOMP 경로에 전달")
    void onMessage_success_routesByRoomIdInPayload() throws Exception {
        String channel = "chat-room-events";

        Message message = mock(Message.class);
        given(message.getChannel()).willReturn(channel.getBytes(StandardCharsets.UTF_8));
        given(message.getBody()).willReturn("{}".getBytes());

        ChatMessageBroadcast broadcast = new ChatMessageBroadcast(
                1L, 5L, "TALK", 10L, "테스터", "안녕하세요", LocalDateTime.now(), null
        );
        given(objectMapper.readValue(any(byte[].class), eq(ChatMessageBroadcast.class))).willReturn(broadcast);

        chatRedisSubscriber.onMessage(message, null);

        verify(messagingTemplate).convertAndSend("/sub/chat/rooms/5", broadcast);
    }

    @Test
    @DisplayName("onMessage 성공 - 채팅방 ID별로 올바른 STOMP 경로로 분리 전달")
    void onMessage_success_routesToCorrectRoomDestination() throws Exception {
        Message message = mock(Message.class);
        given(message.getChannel()).willReturn("chat-room-events".getBytes(StandardCharsets.UTF_8));
        given(message.getBody()).willReturn("{}".getBytes());

        ChatMessageBroadcast broadcast = new ChatMessageBroadcast(
                2L, 99L, "TALK", 20L, "유저B", "메시지", LocalDateTime.now(), null
        );
        given(objectMapper.readValue(any(byte[].class), eq(ChatMessageBroadcast.class))).willReturn(broadcast);

        chatRedisSubscriber.onMessage(message, null);

        verify(messagingTemplate).convertAndSend("/sub/chat/rooms/99", broadcast);
    }

    @Test
    @DisplayName("onMessage 실패 - 역직렬화 예외 발생 시 STOMP 전달하지 않음")
    void onMessage_deserializationFails_doesNotBroadcast() throws Exception {
        Message message = mock(Message.class);
        given(message.getChannel()).willReturn("chat-room-events".getBytes(StandardCharsets.UTF_8));
        given(message.getBody()).willReturn("invalid-json".getBytes());
        given(objectMapper.readValue(any(byte[].class), eq(ChatMessageBroadcast.class)))
                .willThrow(new RuntimeException("역직렬화 실패"));

        chatRedisSubscriber.onMessage(message, null);

        verify(messagingTemplate, org.mockito.Mockito.never())
                .convertAndSend(any(String.class), any(Object.class));
    }
}
