package com.example.fivespringusedmarket.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.fivespringusedmarket.common.exception.ErrorCode;

/**
 * 모든 REST API 응답을 감싸는 공통 응답 객체다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {

    private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공했습니다.";

    public static <T> ApiResponse<T> success(T data) {
        // 기본 성공 메시지로 응답한다.
        return new ApiResponse<>(true, null, DEFAULT_SUCCESS_MESSAGE, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        // API별 성공 메시지가 필요한 경우 사용한다.
        return new ApiResponse<>(true, null, message, data);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        // ErrorCode 기준으로 실패 응답을 생성한다.
        return new ApiResponse<>(false, errorCode.name(), errorCode.getMessage(), null);
    }
}
