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
    BLACKLIST_TOKEN(HttpStatus.UNAUTHORIZED, "이미 로그아웃된 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 Refresh Token입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 채팅
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "채팅방 참여자가 아닙니다."),
    NOT_CS_ROOM(HttpStatus.CONFLICT, "CS 타입이 아닌 채팅방입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 CS 상태 전이입니다."),
    INVALID_CHAT_ROOM_TYPE(HttpStatus.BAD_REQUEST, "type은 TRADE 또는 CS만 허용됩니다."),
    CHAT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 CS 채팅방에는 메시지를 전송할 수 없습니다."),
    CS_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "이미 다른 관리자가 처리 중인 문의입니다."),
    INVALID_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "클라이언트에서 전송할 수 없는 메시지 타입입니다."),
    ADMIN_CANNOT_CHAT(HttpStatus.BAD_REQUEST, "관리자는 구매/판매 채팅을 할수 없습니다."),


    // Product
	INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리 값입니다."),
	INVALID_PRICE(HttpStatus.BAD_REQUEST, "가격은 0 이상이어야 합니다."),
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."),
	CANNOT_MODIFY_SOLD_PRODUCT(HttpStatus.BAD_REQUEST, "판매 완료된 상품은 수정할 수 없습니다."),
    PRODUCT_OWNER_CANNOT_CHAT(HttpStatus.BAD_REQUEST, "본인 상품에는 채팅을 시작할 수 없습니다."),
    PRODUCT_SOLD_OUT(HttpStatus.BAD_REQUEST, "판매 완료되거나 삭제된 상품입니다."),
    INVALID_SEARCH_SORT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 정렬입니다."),

	INVALID_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 값입니다."),
	INVALID_CS_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 상태 전이입니다.");

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
