package com.example.fivespringusedmarket.chat.controller;

import com.example.fivespringusedmarket.chat.dto.request.CsChatRoomCreateRequest;
import com.example.fivespringusedmarket.chat.dto.request.TradeChatRoomCreateRequest;
import com.example.fivespringusedmarket.chat.dto.response.CsChatRoomCreateResponse;
import com.example.fivespringusedmarket.chat.dto.response.TradeChatRoomCreateResponse;
import com.example.fivespringusedmarket.chat.service.ChatService;
import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
