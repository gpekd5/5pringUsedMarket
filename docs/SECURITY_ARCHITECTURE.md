# SECURITY_ARCHITECTURE.md

## 인증 구조

본 프로젝트는 JWT + Redis 기반 인증 방식을 사용한다.

---

# Token 정책

| 구분            | 설명                |
| ------------- | ----------------- |
| Access Token  | API 인증용           |
| Refresh Token | Access Token 재발급용 |

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

---

# Blacklist

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
```

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
