package com.example.fivespringusedmarket.common.security;

import java.security.Principal;

/**
  STOMP CONNECT 시 ChannelInterceptor가 설정하는 Principal 구현체다.
  name 필드에 memberId(String)를 담아 @MessageMapping 핸들러에서 발신자를 식별한다.
 */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
