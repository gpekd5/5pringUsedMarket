# SECURITY_ARCHITECTURE.md

## 인증 구조

본 프로젝트는 JWT + Redis 기반 인증 방식을 사용한다.

Access Token은 짧게 유지하고, Refresh Token은 Redis Whitelist 방식으로 관리한다.
로그아웃 시에는 Refresh Token을 삭제하고, 아직 만료되지 않은 Access Token은 Redis Blacklist에 등록해 즉시 무효화한다.
Refresh Token 재발급에는 Rotation 전략을 적용해 기존 Refresh Token을 폐기하고 새 Refresh Token으로 교체한다.

---

# Token 정책

| 구분            | 만료 시간 | 설명                | Redis 관리 방식 |
| ------------- | ----- | ----------------- | ------------ |
| Access Token  | 30분   | API 인증용           | 로그아웃 시 Blacklist |
| Refresh Token | 14일   | Access Token 재발급용 | Whitelist 저장 |

Access Token Blacklist는 현재 서비스 특성상 반드시 필요한 수준은 아니지만, JWT의 Stateless 특성상 로그아웃 후에도 Access Token이 만료 전까지 사용될 수 있는 문제를 직접 해결하고 Redis 기반 인증 전략을 학습하기 위해 적용한다.

Refresh Token Rotation은 Refresh Token 탈취 시 발생할 수 있는 재사용 공격(Replay Attack) 위험을 줄이기 위해 적용한다.
재발급이 성공하면 Redis에 저장된 Refresh Token을 새로운 값으로 교체하며, 기존 Refresh Token은 더 이상 사용할 수 없다.

---

# 인증 흐름

```text
회원가입
    ↓
로그인
    ↓
Access Token 발급
Refresh Token 발급
    ↓
API 요청
    ↓
JWT Filter 검증
    ↓
인가 처리
```

---

# 로그인

```text
1. 이메일 조회
2. 비밀번호 검증
3. Access Token 생성
4. Refresh Token 생성
5. Refresh Token Redis 저장
6. 토큰 반환
```

---

# Refresh Token 저장

Refresh Token은 Redis에 저장된 값만 유효한 토큰으로 인정하는 Whitelist 방식으로 관리한다.
토큰 재발급 요청 시 클라이언트가 전달한 Refresh Token과 Redis에 저장된 Refresh Token을 비교한다.

Redis Key

```text
refresh-token:{memberId}
```

예시

```text
refresh-token:1
refresh-token:25
```

Value

```text
JWT Refresh Token
```

TTL

```text
14일
```

---

# API 인증

```text
Client
 ↓
Authorization Header
 ↓
JWT Filter
 ↓
Token 검증
 ↓
Blacklist 확인
 ↓
SecurityContext 저장
 ↓
Controller 진입
```

---

# 로그아웃

```text
1. Access Token 추출
2. 남은 만료시간 계산
3. Redis Blacklist 저장
4. Refresh Token 삭제
```

로그아웃이 완료되면 Redis에 저장된 Refresh Token이 삭제되므로 토큰 재발급이 차단된다.
아직 만료되지 않은 Access Token은 Redis Blacklist에 등록해 남은 만료시간 동안 사용할 수 없게 한다.

---

# Blacklist

Access Token Blacklist는 로그아웃된 Access Token을 즉시 무효화하기 위한 저장소다.
Blacklist TTL은 Access Token의 남은 만료시간으로 설정해 토큰 자체가 만료되면 Redis에서도 자동 제거되도록 한다.

Redis Key

```text
blacklist:access-token:{token}
```

예시

```text
blacklist:access-token:eyJhbGc...
```

TTL

```text
Access Token 남은 만료시간
```

---

# 토큰 재발급

```text
Client
 ↓
Refresh Token 전달
 ↓
JWT 검증
 ↓
Redis 조회
 ↓
일치 확인
 ↓
새 Access Token 발급
새 Refresh Token 발급
 ↓
Redis Refresh Token 교체
 ↓
새 토큰 반환
```

재발급 요청에 사용된 Refresh Token이 Redis에 저장된 값과 일치하면 새로운 Access Token과 Refresh Token을 발급한다.
이때 Redis의 Refresh Token 값을 새 Refresh Token으로 교체하므로, 기존 Refresh Token은 즉시 폐기된다.
Redis에 Refresh Token이 없거나 요청 Refresh Token과 일치하지 않으면 재로그인이 필요하다.

---

# Spring Security 구성

```text
SecurityConfig
    ↓
JwtAuthenticationFilter
    ↓
AuthenticationEntryPoint
    ↓
AccessDeniedHandler
```

---

# 권한

```java
MEMBER
ADMIN
```

---

# URL 정책

```text
/api/auth/**
    permitAll

GET /api/products
GET /api/products/{productId}
GET /api/members/{memberId}/profile
    permitAll

GET /api/coupons
    permitAll

GET /api/search/popular
    permitAll

GET /api/v1/products/search
GET /api/v2/products/search
    permitAll (인증 정보가 있으면 최근 검색어 저장)

/api/admin/**
    hasRole("ADMIN")

나머지
    authenticated
```

---

# WebSocket 인증

HTTP Filter는 WebSocket CONNECT 이후 동작하지 않는다.

따라서 STOMP CONNECT 시점에 JWT 인증을 수행한다.

```text
Client
 ↓
STOMP CONNECT
 ↓
ChannelInterceptor
 ↓
JWT 검증
 ↓
Principal 저장
```

---

# Principal 사용

메시지 전송 시

```json
{
  "content": "안녕하세요"
}
```

클라이언트는 senderId를 보내지 않는다.

서버가 Principal에서 사용자 정보를 조회한다.

---

# Redis 사용처

```text
Refresh Token 저장
Access Token Blacklist
Coupon Redis Lock
Popular Keyword ZSet
Chat Redis Pub/Sub
```

---

# 보안 원칙

* 인증 관련 식별자는 Request Body를 신뢰하지 않는다.
* memberId는 JWT에서 추출한다.
* senderId는 Principal에서 추출한다.
* 비밀번호는 BCrypt로 저장한다.
* JWT Secret은 코드에 하드코딩하지 않는다.
* 운영 환경은 Parameter Store 또는 환경변수를 사용한다.
