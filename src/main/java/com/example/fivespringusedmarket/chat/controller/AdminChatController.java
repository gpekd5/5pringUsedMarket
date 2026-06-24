package com.example.fivespringusedmarket.chat.controller;

import com.example.fivespringusedmarket.chat.dto.request.CsStatusUpdateRequest;
import com.example.fivespringusedmarket.chat.dto.response.AdminEnterResponse;
import com.example.fivespringusedmarket.chat.dto.response.CsRoomListResponse;
import com.example.fivespringusedmarket.chat.dto.response.CsStatusUpdateResponse;
import com.example.fivespringusedmarket.chat.entity.CsStatus;
import com.example.fivespringusedmarket.chat.service.AdminChatService;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/chat/rooms/cs")
@RequiredArgsConstructor
public class AdminChatController {

    private final AdminChatService adminChatService;

    /*
      관리자가 CS 채팅방에 입장한다.
      ChatMember에 관리자를 추가하고 csStatus를 WAITING→IN_PROGRESS로 자동 변경한다
      이후 클라이언트는 STOMP ENTER를 별도로 호출해 시스템 메시지를 브로드캐스트한다
     */
    @PostMapping("/{roomId}/enter")
    public ResponseEntity<ApiResponse<AdminEnterResponse>> enterCsRoom(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long roomId
    ) {
        AdminEnterResponse response = adminChatService.adminEnterCsRoom(roomId, authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success("CS 채팅방 입장 성공", response));
    }

    /**
      CS 문의 채팅방 상태를 변경
      변경 후 고객에게 STOMP 알림이 발송
     */
    @PatchMapping("/{roomId}/status")
    public ResponseEntity<ApiResponse<CsStatusUpdateResponse>> changeCsStatus(
            @PathVariable Long roomId,
            @Valid @RequestBody CsStatusUpdateRequest request
    ) {
        CsStatus newStatus = parseCsStatus(request.status());
        CsStatusUpdateResponse response = adminChatService.changeCsStatus(roomId, newStatus);
        return ResponseEntity.ok(ApiResponse.success("CS 상태 변경 성공", response));
    }

    /*파싱 작업이기때문에 컨트롤러에서 처리한다
    컨트롤러 — 클라이언트 요청값이 올바른 형식인지 검증 (String → Enum 변환 포함)
    서비스 — 비즈니스 로직 처리 (변환된 값으로 상태 전이 검증)
    서비스가 변환된 타입을 받는게 역활 정책상 맞다
     */
    private CsStatus parseCsStatus(String status) {
        try {
            return CsStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_CS_STATUS_TRANSITION);
        }
    }

    /*
  관리자용 CS 채팅방 목록 조회
  기본 필터: WAITING, status=ALL 파라미터로 전체 조회 가능
 */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CsRoomListResponse>>> getCsRooms(
            @RequestParam(defaultValue = "WAITING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        CsStatus csStatus = "ALL".equalsIgnoreCase(status) ? null : parseCsStatus(status);
        Page<CsRoomListResponse> response = adminChatService.getCsRooms(csStatus, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("CS 채팅방 목록 조회 성공", response));
    }

}

