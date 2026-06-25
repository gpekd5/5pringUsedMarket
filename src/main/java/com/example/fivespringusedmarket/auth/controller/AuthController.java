package com.example.fivespringusedmarket.auth.controller;

import com.example.fivespringusedmarket.auth.dto.LoginRequest;
import com.example.fivespringusedmarket.auth.dto.ReissueRequest;
import com.example.fivespringusedmarket.auth.dto.SignupRequest;
import com.example.fivespringusedmarket.auth.dto.SignupResponse;
import com.example.fivespringusedmarket.auth.dto.TokenResponse;
import com.example.fivespringusedmarket.auth.service.AuthService;
import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.common.security.BearerTokenResolver;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입과 로그인을 처리하는 인증 API Controller다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final BearerTokenResolver bearerTokenResolver;

    public AuthController(AuthService authService, BearerTokenResolver bearerTokenResolver) {
        this.authService = authService;
        this.bearerTokenResolver = bearerTokenResolver;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", response));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody ReissueRequest request) {
        TokenResponse response = authService.reissue(request);
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AuthMember authMember,
            HttpServletRequest request
    ) {
        String accessToken = bearerTokenResolver.resolve(request);
        authService.logout(authMember.memberId(), accessToken);

        return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다.", null));
    }
}
