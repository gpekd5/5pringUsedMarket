package com.example.fivespringusedmarket.chat.dto.response;

/*
  관리자 CS 채팅방 입장 REST API 응답 DTO
  입장 직후 자동으로 IN_PROGRESS로 변경된 csStatus를 포함
 */
public record AdminEnterResponse(
        Long roomId,
        String title,
        String csStatus
) {
}
