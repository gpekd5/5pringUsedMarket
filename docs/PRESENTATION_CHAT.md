# 실시간 채팅 구현기
## WebSocket + STOMP + Redis Pub/Sub

---

## 1. 왜 WebSocket을 선택했나요?

**HTTP의 한계**
- 클라이언트가 먼저 요청해야만 서버가 응답할 수 있음
- 채팅을 HTTP로 구현하려면 "새 메시지 있어요?" 를 계속 물어봐야 함 (폴링)

```
HTTP 폴링:
클라이언트 → "새 메시지 있어?" → 서버 (없음)
클라이언트 → "새 메시지 있어?" → 서버 (없음)
클라이언트 → "새 메시지 있어?" → 서버 (있음!) → 클라이언트
```

**WebSocket의 해결**
- 한 번 연결하면 서버가 먼저 메시지를 보낼 수 있음
- 불필요한 요청 없이 메시지가 오면 즉시 수신

```
WebSocket:
클라이언트 ←——— 연결 유지 ———→ 서버
                                  ↓ 메시지 오면
클라이언트 ← 즉시 전달
```

---

## 2. STOMP를 추가로 도입한 이유는?

**순수 WebSocket만으로 구현할 때의 문제**
- 누구한테 보낼지 직접 관리해야 함
- 채팅방이 100개면 세션 100개를 직접 추적

**STOMP가 해결하는 것**
- `/sub/chat/rooms/5` 구독하면 5번 채팅방 메시지만 자동 수신
- 발신 `/pub/chat/rooms/5/messages`, 수신 `/sub/chat/rooms/5` 경로로 역할 분리
- Spring에서 `@MessageMapping`으로 HTTP Controller처럼 라우팅 가능

---

## 3. 채팅 도메인은 어떻게 설계했나요?

**ERD 구조**
```
Member ──── ChatMember ──── ChatRoom ──── ChatMessage
               │                │
           (role: MEMBER    (type: TRADE / CS)
                ADMIN)      (csStatus: WAITING
                                       IN_PROGRESS
                                       COMPLETED)
```

**핵심 설계 결정**

| 결정 | 이유 |
|---|---|
| `ChatMember` 중간 테이블 | 참여자 역할(MEMBER/ADMIN), unreadCount 관리를 위해 분리 |
| 거래방 / CS방 분리 | 상태 전이 규칙, 참여자 구성이 달라 타입으로 분기 |
| 커서 기반 페이징 | 메시지 조회 중 새 메시지가 추가돼도 페이지가 밀리지 않음 |
| CS 상태 전이 엔티티 내부 검증 | `WAITING → IN_PROGRESS → COMPLETED` 규칙을 `ChatRoom.changeCsStatus()`에서 강제 |

---

## 4. 분산 서버 환경에서 메시지 전달은?

**단일 서버에서는 괜찮았는데 왜 2대에서 문제가 될까?**

```
[단일 서버]
유저A, 유저B 모두 같은 서버 연결
→ 메시지 수신 시 서버가 둘 다 알고 있어서 전달 가능 ✅

[서버 2대]
유저A → 서버1 연결
유저B → 서버2 연결
유저A가 메시지 전송 → 서버1은 서버2에 연결된 유저B를 모름 ❌
```

**Redis Pub/Sub으로 해결**

```
변경 전:
클라이언트 → 서버 → 현재 서버 구독자에게만 전달

변경 후:
클라이언트 → 서버1 → Redis "chat-room:5" 채널에 발행
                       ↓
          서버1 Subscriber → 서버1 구독자에게 전달
          서버2 Subscriber → 서버2 구독자에게 전달
```

**구현 포인트**
- `ChatRedisPublisher` — 메시지를 Redis 채널에 발행
- `ChatRedisSubscriber` — Redis 채널 구독, 수신 시 STOMP로 최종 전달
- `PatternTopic("chat-room:*")` — 채팅방 수에 관계없이 단일 구독으로 처리

---

## 5. 사용자 인증은 어떻게 처리했나요?

**HTTP Filter로 충분하지 않은 이유**

```
HTTP Filter → WebSocket 업그레이드 요청만 검사
STOMP CONNECT → Filter 통과 후 발생 → Filter가 검사 못함
```

- 로그아웃(블랙리스트) 처리된 토큰도 STOMP CONNECT 가능한 보안 취약점 존재

**StompChannelInterceptor에서 처리**

```
STOMP CONNECT 프레임
    → StompChannelInterceptor.preSend()
    → JWT 유효성 검증
    → Redis 블랙리스트 토큰 여부 확인
    → Principal 설정 (memberId)

STOMP SUBSCRIBE 프레임
    → 구독하려는 roomId의 ChatMember 존재 여부 확인
    → 참여자가 아니면 구독 차단
```

---

## 트러블슈팅

**1. CS 상태 변경 시 unreadCount 버그**
- `changeCsStatus()`에서 미정의 변수 `senderId` 참조로 컴파일 에러
- `ChatMemberRole.MEMBER` 필터로 고객만 찾아 unreadCount 증가하도록 수정

**2. 관리자 동시 입장 문제**
- 두 관리자가 동시에 같은 CS방 입장 시도 시 둘 다 입장 가능한 문제
- `SELECT FOR UPDATE` 비관적 락으로 해결

---

## 아키텍처 구성도

```
[클라이언트]
    │  WebSocket / STOMP
    ▼
[서버 1]                    [서버 2]
StompChannelInterceptor     StompChannelInterceptor
    │ JWT 검증                  │ JWT 검증
    ▼                           ▼
StompController             StompController
    │                           │
ChatRedisPublisher          ChatRedisPublisher
    │                           │
    └──────→ [Redis] ←──────────┘
                │
    ┌───────────┴───────────┐
    ▼                       ▼
ChatRedisSubscriber    ChatRedisSubscriber
(서버1)                (서버2)
    │                       │
STOMP 브로드캐스트      STOMP 브로드캐스트
    │                       │
[서버1 구독자]          [서버2 구독자]
```
