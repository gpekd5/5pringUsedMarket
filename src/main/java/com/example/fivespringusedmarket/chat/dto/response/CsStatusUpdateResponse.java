package com.example.fivespringusedmarket.chat.dto.response;

/*
  CS 상태 변경 응답 DTO
 */
public record CsStatusUpdateResponse(
        Long roomId,
        String csStatus
) {
}

