package com.example.fivespringusedmarket.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 프로젝트 전역에서 사용하는 내부 에러 코드를 정의한다.
 */
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

	// Product
	INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리 값입니다."),
	INVALID_PRICE(HttpStatus.BAD_REQUEST, "가격은 0 이상이어야 합니다."),
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."),
	CANNOT_MODIFY_SOLD_PRODUCT(HttpStatus.BAD_REQUEST, "판매 완료된 상품은 수정할 수 없습니다."),
	INVALID_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 값입니다."),
	INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 상태 전이입니다.");
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
