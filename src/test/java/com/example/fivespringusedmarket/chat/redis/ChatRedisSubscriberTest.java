package com.example.fivespringusedmarket.chat.redis;

import com.example.fivespringusedmarket.chat.dto.response.ChatMessageBroadcast;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
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
    @DisplayName("onMessage 성공 - pattern이 아닌 message.getChannel()에서 roomId를 파싱해 올바른 STOMP 경로로 전달")
    void onMessage_success_parsesRoomIdFromChannel() throws Exception {
        // pattern은 구독 패턴(chat-room:*)이고 실제 채널명은 message.getChannel()에 있다
        String actualChannel = "chat-room:5";
        String patternString = "chat-room:*"; // pattern은 항상 구독 패턴

        Message message = mock(Message.class);
        given(message.getChannel()).willReturn(actualChannel.getBytes(StandardCharsets.UTF_8));
        given(message.getBody()).willReturn("{}".getBytes());

        ChatMessageBroadcast broadcast = new ChatMessageBroadcast(
                1L, 5L, "TALK", 10L, "테스터", "안녕하세요", LocalDateTime.now(), null
        );
        given(objectMapper.readValue(any(byte[].class), eq(ChatMessageBroadcast.class))).willReturn(broadcast);

        chatRedisSubscriber.onMessage(message, patternString.getBytes(StandardCharsets.UTF_8));

        // pattern("chat-room:*")이 아닌 실제 채널명("chat-room:5")에서 파싱한 roomId로 전달되어야 한다
        verify(messagingTemplate).convertAndSend("/sub/chat/rooms/5", broadcast);
    }

    @Test
    @DisplayName("onMessage 성공 - 채팅방 ID별로 올바른 STOMP 경로로 분리 전달")
    void onMessage_success_routesToCorrectRoomDestination() throws Exception {
        Message message = mock(Message.class);
        given(message.getChannel()).willReturn("chat-room:99".getBytes(StandardCharsets.UTF_8));
        given(message.getBody()).willReturn("{}".getBytes());

        ChatMessageBroadcast broadcast = new ChatMessageBroadcast(
                2L, 99L, "TALK", 20L, "유저B", "메시지", LocalDateTime.now(), null
        );
        given(objectMapper.readValue(any(byte[].class), eq(ChatMessageBroadcast.class))).willReturn(broadcast);

        chatRedisSubscriber.onMessage(message, "chat-room:*".getBytes(StandardCharsets.UTF_8));

        verify(messagingTemplate).convertAndSend("/sub/chat/rooms/99", broadcast);
    }

    @Test
    @DisplayName("onMessage 실패 - 역직렬화 예외 발생 시 STOMP 전달하지 않음")
    void onMessage_deserializationFails_doesNotBroadcast() throws Exception {
        Message message = mock(Message.class);
        given(message.getChannel()).willReturn("chat-room:5".getBytes(StandardCharsets.UTF_8));
        given(message.getBody()).willReturn("invalid-json".getBytes());
        given(objectMapper.readValue(any(byte[].class), eq(ChatMessageBroadcast.class)))
                .willThrow(new RuntimeException("역직렬화 실패"));

        chatRedisSubscriber.onMessage(message, "chat-room:*".getBytes(StandardCharsets.UTF_8));

        verify(messagingTemplate, org.mockito.Mockito.never())
                .convertAndSend(any(String.class), any(Object.class));
    }
}
