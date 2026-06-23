package com.example.fivespringusedmarket.chat.entity;

/**
 * CS 문의 채팅방의 처리 상태를 나타낸다.
 * WAITING: 대기 중 (생성 초기 상태)
 * IN_PROGRESS: 관리자가 처리 중
 * COMPLETED: 처리 완료
 */
public enum CsStatus {
    WAITING,
    IN_PROGRESS,
    COMPLETED
}
