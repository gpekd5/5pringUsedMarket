package com.example.fivespringusedmarket.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CS 문의 채팅방 생성 요청 DTO
 */
public record CsChatRoomCreateRequest(
        @NotBlank @Size(max = 100) String title
) {
}
