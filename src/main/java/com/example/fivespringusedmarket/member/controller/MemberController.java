package com.example.fivespringusedmarket.member.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.member.dto.MemberMeRequest;
import com.example.fivespringusedmarket.member.dto.MemberMeResponse;
import com.example.fivespringusedmarket.member.dto.MemberMeUpdateResponse;
import com.example.fivespringusedmarket.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 정보를 조회 및 수정하는 API Controller다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> getMyInfo(@AuthenticationPrincipal AuthMember authMember) {
        MemberMeResponse response = memberService.getInfo(authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success("내 정보 조회에 성공했습니다.", response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeUpdateResponse>> updateMyInfo(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MemberMeRequest request
    ) {
        MemberMeUpdateResponse response = memberService.updateInfo(
                authMember.memberId(),
                request.nickname()
        );
        return ResponseEntity.ok(ApiResponse.success("내 정보가 수정되었습니다.", response));
    }
}
