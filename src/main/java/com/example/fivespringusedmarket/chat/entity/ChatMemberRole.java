package com.example.fivespringusedmarket.chat.entity;

/**
  채팅방 내 참여자 역할을 나타낸다.
  중고거래 특성상 모든 유저가 판매자·구매자가 될 수 있으므로 MEMBER/ADMIN 두 가지만 사용한다.
  MEMBER: 일반 사용자 (거래 채팅의 구매자·판매자, CS 채팅의 고객)
  ADMIN: 관리자 (CS 채팅 전용)
 */
public enum ChatMemberRole {
    MEMBER,
    ADMIN
}
