package com.example.fivespringusedmarket.chat.dto.request;

import com.example.fivespringusedmarket.chat.entity.MessageType;
import jakarta.validation.constraints.NotNull;

/*
  STOMP 메시지 전송 요청 DTO다.
  type 필드로 TALK / ENTER / LEAVE를 구분하여 /pub/chat/{roomId} 단일 경로로 전송한다.
  TALK일 때만 content가 필요하고, ENTER / LEAVE는 content를 null로 보낸다.
  senderId는 서버가 Principal에서 추출하므로 클라이언트는 포함하지 않는다.
 */
public record ChatSendRequest(
        @NotNull MessageType type,
        String content
) {
}
