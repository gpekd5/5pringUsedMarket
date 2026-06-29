package com.example.fivespringusedmarket.mypage.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.mypage.dto.MyPageResponse;
import com.example.fivespringusedmarket.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 요약 조회 API를 제공한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

    private final MyPageService myPageService;

    /**
     * 인증된 회원의 마이페이지 첫 화면 요약 정보를 조회한다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        MyPageResponse response = myPageService.getMyPage(authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success("마이페이지 조회에 성공했습니다.", response));
    }
}
