package com.example.fivespringusedmarket.chat.controller;

import com.example.fivespringusedmarket.chat.dto.request.CsChatRoomCreateRequest;
import com.example.fivespringusedmarket.chat.dto.request.TradeChatRoomCreateRequest;
import com.example.fivespringusedmarket.chat.dto.response.*;
import com.example.fivespringusedmarket.chat.service.ChatService;
import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<TradeChatRoomCreateResponse>> createTradeRoom(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody TradeChatRoomCreateRequest request
    ) {
        TradeChatRoomCreateResponse response = chatService.findOrCreateTradeRoom(authMember.memberId(), request);

        return ResponseEntity.ok(ApiResponse.success("채팅방 조회/생성 성공", response));
    }
    /**
     * CS 문의 채팅방 생성.
     */
    @PostMapping("/cs")
    public ResponseEntity<ApiResponse<CsChatRoomCreateResponse>> createCsRoom(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody CsChatRoomCreateRequest request
    ) {
        CsChatRoomCreateResponse response = chatService.createCsRoom(authMember.memberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CS 문의 채팅방 생성 성공", response));
    }

    /**
     * 채팅방 목록 조회
      type 파라미터 TRADE / CS 필터링이 가능
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ChatRoomListResponse>>> getChatRooms(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ChatRoomListResponse> response = chatService.getChatRooms(
                authMember.memberId(), PageRequest.of(page, size)
        );
        return ResponseEntity.ok(ApiResponse.success("채팅방 목록 조회 성공", response));
    }

    /**
     * 채팅방 상세 조회.
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> getChatRoomDetail(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long roomId
    ) {
        ChatRoomDetailResponse response = chatService.getChatRoomDetail(authMember.memberId(), roomId);
        return ResponseEntity.ok(ApiResponse.success("채팅방 조회 성공", response));
    }

    /**
     * 메시지 목록 조회 (커서 기반 페이징).
     */
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<MessageListResponse>> getMessages(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "20") int size
    ) {
        MessageListResponse response = chatService.getMessages(
                authMember.memberId(), roomId, lastMessageId, size
        );
        return ResponseEntity.ok(ApiResponse.success("메시지 목록 조회 성공", response));
    }
}
