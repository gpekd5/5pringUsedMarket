package com.example.fivespringusedmarket.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record TradeChatRoomCreateRequest(
        @NotNull Long productId
) {
}