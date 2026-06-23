# PACKAGE_STRUCTURE.md

## 프로젝트 패키지 구조

본 프로젝트는 부트캠프 팀 프로젝트 특성상 복잡한 DDD 구조보다 **기능 중심 패키지 구조**를 사용한다.

패키지는 기능별로 분리하고, 각 기능 내부에서 역할별로 구분한다.

---

# 최종 패키지 구조

```text
com.example.fivespringusedmarket

├── common
│   ├── config
│   ├── entity
│   ├── exception
│   ├── response
│   └── security
│
├── auth
│   ├── controller
│   ├── service
│   └── dto
│
├── member
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── product
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── search
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── wish
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── chat
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── mypage
│   ├── controller
│   ├── service
│   └── dto
│
└── coupon
    ├── controller
    ├── service
    ├── repository
    ├── dto
    └── entity
```

---

# common 패키지

공통 기능을 관리한다.

```text
common
├── config
├── entity
├── exception
├── response
└── security
```

## config

프로젝트 설정

예시

```text
SecurityConfig
RedisConfig
WebSocketConfig
CacheConfig
SwaggerConfig
```

## entity

공통 생성·수정 시간을 관리한다.
추후 소프트 삭제 정책이 확정되면 삭제 시간도 함께 관리할 수 있도록 `BaseEntity`를 사용한다.

```text
BaseEntity
```

## exception

예외 처리

예시

```text
CustomException
ErrorCode
GlobalExceptionHandler
```

## response

공통 응답 객체

예시

```text
ApiResponse
```

## security

JWT 인증 필터, 인증 Principal, 인증 실패 응답 등 공통 보안 구조를 관리한다.

---

# auth 패키지

인증/인가 전용 패키지

담당자: 김현승

```text
auth
├── controller
├── service
└── dto
```

주요 기능

* 회원가입
* 로그인
* 로그아웃
* 토큰 재발급
* JWT 발급
* JWT 검증

---

# member 패키지

회원 도메인

담당자: 김현승

```text
member
├── controller
├── service
├── repository
├── dto
└── entity
```

주요 기능

* 내 정보 조회
* 내 정보 수정

---

# product 패키지

상품 도메인

담당자: 상품 담당자

```text
product
├── controller
├── service
├── repository
├── dto
└── entity
```

주요 기능

* 상품 등록
* 상품 목록 조회
* 상품 상세 조회
* 상품 수정
* 상품 삭제
* 판매 상태 변경

---

# search 패키지

검색 및 캐시

담당자: 김홍기

```text
search
├── controller
├── service
├── repository
├── dto
└── entity
```

주요 기능

* 상품 검색
* 최근 검색어
* 인기 검색어
* 캐싱

---

# wish 패키지

관심상품

담당자: 상품 담당자

```text
wish
├── controller
├── service
├── repository
├── dto
└── entity
```

주요 기능

* 관심상품 등록
* 관심상품 취소
* 관심상품 조회

---

# coupon 패키지

쿠폰 및 동시성

담당자: 황정후

```text
coupon
├── controller
├── service
├── repository
├── dto
└── entity
```

주요 기능

* 이벤트 쿠폰 조회
* 선착순 쿠폰 발급
* Redis Lock
* 동시성 테스트

---

# chat 패키지

실시간 채팅

담당자: 윤영범

```text
chat
├── controller
├── service
├── repository
├── dto
└── entity
```

주요 기능

* 거래 채팅
* CS 문의 채팅
* 메시지 저장
* 채팅방 조회
* WebSocket
* STOMP
* Redis Pub/Sub

---

# mypage 패키지

마이페이지 첫 화면의 요약 조회를 담당한다.

```text
mypage
├── controller
├── service
└── dto
```

상세 목록은 마이페이지에서 직접 구현하지 않고 Product, Wish, Coupon, Chat API를 사용한다.

---

# 의존 방향

모든 기능은 아래 방향을 따른다.

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
```

---

# 금지 사항

```text
Controller → Repository 직접 호출 금지
Controller → 비즈니스 로직 작성 금지
Entity 직접 응답 금지
DTO를 Entity처럼 사용 금지
Request Body의 memberId 신뢰 금지
Request Body의 senderId 신뢰 금지
```

---

# 현재 프로젝트 원칙

* 기능 중심 패키지 구조를 사용한다.
* 과도한 DDD 구조는 사용하지 않는다.
* Controller는 최대한 얇게 유지한다.
* 비즈니스 로직은 Service에 작성한다.
* Entity는 상태 변경 책임만 가진다.
* 공통 예외와 응답은 common 패키지에서 관리한다.
* 인증/인가와 공통은 김현승 담당이다.
* 동시성은 황정후 담당이다.
* 캐싱은 김홍기 담당이다.
* 실시간 채팅은 윤영범 담당이다.
