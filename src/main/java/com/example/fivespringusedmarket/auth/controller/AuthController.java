package com.example.fivespringusedmarket.auth.controller;

import com.example.fivespringusedmarket.auth.dto.LoginRequest;
import com.example.fivespringusedmarket.auth.dto.LoginResponse;
import com.example.fivespringusedmarket.auth.dto.SignupRequest;
import com.example.fivespringusedmarket.auth.dto.SignupResponse;
import com.example.fivespringusedmarket.auth.service.AuthService;
import com.example.fivespringusedmarket.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", response));
    }
}
