package com.example.fivespringusedmarket.common.config;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
  STOMP 세션과 채팅방의 매핑을 관리한다.
  비정상 퇴장 시 어느 방에 있었는지 찾기 위해 사용한다.
  ConcurrentHashMap으로 다중 스레드 환경에서 안전하게 접근한다.
 */
@Component
public class StompSessionRegistry {

    // sessionId → 채팅방 정보
    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public void register(String sessionId, Long memberId, Long roomId) {
        sessions.put(sessionId, new SessionInfo(memberId, roomId));
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    public SessionInfo get(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 세션 하나에 대한 멤버 ID와 채팅방 ID를 묶는다.
     */
    public record SessionInfo(Long memberId, Long roomId) {}
}
