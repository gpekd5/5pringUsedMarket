package com.example.fivespringusedmarket.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

/*
  CS 상태 변경 요청 DTO
  status 값은 서비스에서 CsStatus enum으로 변환
*/
public record CsStatusUpdateRequest(
        @NotBlank String status
) {
}
